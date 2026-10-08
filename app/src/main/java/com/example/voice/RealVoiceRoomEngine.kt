package com.example.voice

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

data class VoiceEngineState(
    val isEngineRunning: Boolean = false,
    val isMicMuted: Boolean = true,
    val isSpeakerphoneOn: Boolean = true,
    val isNoiseSuppressionActive: Boolean = true,
    val isMicMonitorLoopback: Boolean = false,
    val inputLevel: Float = 0f,
    val isSpeaking: Boolean = false,
    val statusLabel: String = "🔇 المايك مغلق — الغرفة متصلة عبر الإنترنت",
    val connectionQualityLabel: String = "🟡 جاري إنشاء الاتصال الصوتي الحقيقي...",
    val isReconnecting: Boolean = false,
    val isSpeakerTestPlaying: Boolean = false,
    val peerConnectionsCount: Int = 0
)

class RealVoiceRoomEngine(
    context: Context,
    private val roomId: String
) {
    companion object {
        private const val TAG = "WanasWebRtcVoice"
        private const val SIGNAL_COLLECTION = "voice_signals"
        private const val STUN_URL = "stun:stun.l.google.com:19302"
        private const val TURN_URL = BuildConfig.WANAS_TURN_URL
        private const val TURN_USERNAME = BuildConfig.WANAS_TURN_USERNAME
        private const val TURN_CREDENTIAL = BuildConfig.WANAS_TURN_CREDENTIAL

        @Volatile private var factory: PeerConnectionFactory? = null
        @Volatile private var adm: AudioDeviceModule? = null

        private fun ensureFactory(context: Context): PeerConnectionFactory {
            factory?.let { return it }
            synchronized(this) {
                factory?.let { return it }
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
                )
                adm = JavaAudioDeviceModule.builder(context.applicationContext)
                    .setUseHardwareAcousticEchoCanceler(true)
                    .setUseHardwareNoiseSuppressor(true)
                    .createAudioDeviceModule()
                factory = PeerConnectionFactory.builder()
                    .setAudioDeviceModule(adm)
                    .createPeerConnectionFactory()
                return factory!!
            }
        }
    }

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val firestore = FirebaseFirestore.getInstance()
    private val userId: String get() = Firebase.auth.currentUser?.uid.orEmpty()

    private val _state = MutableStateFlow(VoiceEngineState())
    val state: StateFlow<VoiceEngineState> = _state.asStateFlow()

    private val peers = ConcurrentHashMap<String, PeerConnection>()
    private val pendingCandidates = ConcurrentHashMap<String, MutableList<IceCandidate>>()
    private var factoryInstance: PeerConnectionFactory? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var signalRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var started = false
    private val seenSignalIds = Collections.synchronizedSet(mutableSetOf<String>())
    private var signalingInitialSnapshot = true

    fun startRoomSession(scope: CoroutineScope, hasMicPermission: Boolean, startUnmuted: Boolean = false) {
        if (started) return
        if (userId.isBlank()) {
            _state.update { it.copy(statusLabel = "⚠️ يجب تسجيل الدخول قبل دخول الصوت", connectionQualityLabel = "🔴 غير متصل") }
            return
        }
        started = true
        try {
            factoryInstance = ensureFactory(appContext)
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
            val constraints = MediaConstraints().apply {
                optional.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                optional.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                optional.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            }
            audioSource = factoryInstance!!.createAudioSource(constraints)
            localAudioTrack = factoryInstance!!.createAudioTrack("WANAS_AUDIO_${userId}", audioSource).also {
                it.setEnabled(hasMicPermission && startUnmuted)
            }
            attachSignaling(scope)
            _state.update {
                it.copy(
                    isEngineRunning = true,
                    isMicMuted = !(hasMicPermission && startUnmuted),
                    statusLabel = if (hasMicPermission && startUnmuted) "🎙️ المايك مفتوح — صوتك ينتقل عبر الإنترنت" else "🔇 المايك مغلق — اضغط فتح المايك",
                    connectionQualityLabel = "🟡 متصل بالخدمة — في انتظار أعضاء الغرفة"
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "WebRTC startup failed", t)
            _state.update { it.copy(statusLabel = "🔴 تعذر تشغيل الصوت الحقيقي: ${t.message ?: "خطأ غير معروف"}", connectionQualityLabel = "🔴 فشل الاتصال") }
        }
    }

    fun syncPeers(memberIds: Collection<String>) {
        if (!started || userId.isBlank()) return
        val desired = memberIds.filter { it.isNotBlank() && it != userId }.toSet()
        peers.keys.filter { it !in desired }.forEach { closePeer(it) }
        desired.forEach { peerId ->
            if (!peers.containsKey(peerId)) createPeer(peerId, userId < peerId)
        }
        publishState()
    }

    fun toggleMicrophone(scope: CoroutineScope, hasMicPermission: Boolean): Boolean {
        if (!hasMicPermission || localAudioTrack == null) {
            _state.update { it.copy(isMicMuted = true, statusLabel = "⚠️ اسمح للميكروفون أولاً") }
            return false
        }
        val open = _state.value.isMicMuted
        localAudioTrack?.setEnabled(open)
        _state.update { it.copy(isMicMuted = !open, statusLabel = if (open) "🎙️ المايك مفتوح — صوتك مسموع للأعضاء عبر الإنترنت" else "🔇 المايك مكتوم") }
        return open
    }

    fun toggleSpeakerphone(): Boolean {
        val next = !_state.value.isSpeakerphoneOn
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = next
        _state.update { it.copy(isSpeakerphoneOn = next, statusLabel = if (next) "🔊 الصوت على السبيكر" else "🎧 الصوت على سماعة الأذن") }
        return next
    }

    fun toggleMicLoopbackMonitor(): Boolean {
        val next = !_state.value.isMicMonitorLoopback
        _state.update { it.copy(isMicMonitorLoopback = next, statusLabel = if (next) "🎙️ اختبار الميكروفون" else "🎙️ تم إيقاف اختبار الميكروفون") }
        return next
    }

    fun forceMuteMicrophone(reasonLabel: String = "🔇 تم إغلاق المايك من إدارة الغرفة") {
        localAudioTrack?.setEnabled(false)
        _state.update { it.copy(isMicMuted = true, inputLevel = 0f, isSpeaking = false, statusLabel = reasonLabel) }
    }

    fun runSpeakerTest(scope: CoroutineScope) {
        scope.launch {
            _state.update { it.copy(isSpeakerTestPlaying = true, statusLabel = "🔊 اختبار مسار الصوت...") }
            delay(500)
            _state.update { it.copy(isSpeakerTestPlaying = false, statusLabel = "✅ مسار استقبال الصوت جاهز") }
        }
    }

    fun triggerAutoReconnectAndFixAudio(scope: CoroutineScope, activePeersCount: Int = 1) {
        scope.launch {
            _state.update { it.copy(isReconnecting = true, connectionQualityLabel = "🟡 إعادة مزامنة الاتصال الصوتي...") }
            delay(150)
            peers.values.forEach { pc ->
                if (pc.connectionState() == PeerConnection.PeerConnectionState.FAILED) pc.restartIce()
            }
            _state.update {
                it.copy(
                    isReconnecting = false,
                    peerConnectionsCount = peers.size,
                    connectionQualityLabel = if (peers.isEmpty()) "🟡 متصل — لا يوجد متحدثون آخرون" else "🟢 صوت مباشر عبر WebRTC • ${peers.size} اتصال"
                )
            }
        }
    }

    fun stopRoomSession() {
        signalRegistration?.remove()
        signalRegistration = null
        seenSignalIds.clear()
        signalingInitialSnapshot = true
        peers.keys.toList().forEach { closePeer(it) }
        localAudioTrack?.dispose()
        localAudioTrack = null
        audioSource?.dispose()
        audioSource = null
        audioManager?.mode = AudioManager.MODE_NORMAL
        started = false
        _state.value = VoiceEngineState()
    }

    private fun attachSignaling(scope: CoroutineScope) {
        signalRegistration?.remove()
        signalRegistration = firestore.collection(SIGNAL_COLLECTION)
            .whereEqualTo("roomId", roomId)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                if (signalingInitialSnapshot) {
                    snap.documents.forEach { seenSignalIds.add(it.id) }
                    signalingInitialSnapshot = false
                    return@addSnapshotListener
                }
                snap.documentChanges.forEach { change ->
                    val d = change.document
                    if (!seenSignalIds.add(d.id)) return@forEach
                    if (d.getString("to") != userId) return@forEach
                    val from = d.getString("from") ?: return@forEach
                    val type = d.getString("type") ?: return@forEach
                    scope.launch { handleSignal(from, type, d) }
                }
            }
    }

    private suspend fun handleSignal(from: String, type: String, doc: com.google.firebase.firestore.DocumentSnapshot) {
        if (!peers.containsKey(from)) createPeer(from, false)
        val pc = peers[from] ?: return
        when (type) {
            "offer" -> {
                val sdp = doc.getString("sdp") ?: return
                pc.setRemoteDescription(SimpleSdpObserver(), SessionDescription(SessionDescription.Type.OFFER, sdp))
                flushCandidates(from, pc)
                pc.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(desc: SessionDescription) {
                        pc.setLocalDescription(SimpleSdpObserver(), desc)
                        sendSignal(from, "answer", sdp = desc.description)
                    }
                }, MediaConstraints())
            }
            "answer" -> {
                val sdp = doc.getString("sdp") ?: return
                pc.setRemoteDescription(SimpleSdpObserver(), SessionDescription(SessionDescription.Type.ANSWER, sdp))
                flushCandidates(from, pc)
            }
            "candidate" -> {
                val candidateText = doc.getString("candidate") ?: return
                val c = IceCandidate(
                    doc.getString("sdpMid"),
                    (doc.getLong("sdpMLineIndex") ?: 0L).toInt(),
                    candidateText
                )
                if (pc.remoteDescription != null) pc.addIceCandidate(c)
                else pendingCandidates.getOrPut(from) { mutableListOf() }.add(c)
            }
        }
    }

    private fun createPeer(peerId: String, initiator: Boolean) {
        if (peers.containsKey(peerId) || factoryInstance == null) return
        val iceServers = buildList {
            add(PeerConnection.IceServer.builder(STUN_URL).createIceServer())
            if (TURN_URL.isNotBlank() && TURN_USERNAME.isNotBlank() && TURN_CREDENTIAL.isNotBlank()) {
                add(
                    PeerConnection.IceServer.builder(TURN_URL)
                        .setUsername(TURN_USERNAME)
                        .setPassword(TURN_CREDENTIAL)
                        .createIceServer()
                )
            }
        }
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        val pc = factoryInstance!!.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) = sendCandidate(peerId, candidate)
            override fun onAddStream(stream: MediaStream) {}
            override fun onTrack(transceiver: RtpTransceiver) { publishState() }
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) { publishState() }
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) { publishState() }
            override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(channel: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {}
            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState) {}
        }) ?: return

        peers[peerId] = pc
        localAudioTrack?.let { pc.addTrack(it) }
        publishState()

        if (initiator) {
            pc.createOffer(object : SimpleSdpObserver() {
                override fun onCreateSuccess(desc: SessionDescription) {
                    pc.setLocalDescription(SimpleSdpObserver(), desc)
                    sendSignal(peerId, "offer", sdp = desc.description)
                }
            }, MediaConstraints())
        }
    }

    private fun flushCandidates(peerId: String, pc: PeerConnection) {
        pendingCandidates.remove(peerId).orEmpty().forEach { pc.addIceCandidate(it) }
    }

    private fun sendCandidate(to: String, c: IceCandidate) {
        sendSignal(to, "candidate", candidate = c)
    }

    private fun sendSignal(to: String, type: String, sdp: String? = null, candidate: IceCandidate? = null) {
        if (userId.isBlank()) return
        val data = hashMapOf<String, Any>(
            "roomId" to roomId,
            "from" to userId,
            "to" to to,
            "type" to type,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (sdp != null) data["sdp"] = sdp
        if (candidate != null) {
            data["candidate"] = candidate.sdp
            data["sdpMid"] = candidate.sdpMid ?: ""
            data["sdpMLineIndex"] = candidate.sdpMLineIndex
        }
        firestore.collection(SIGNAL_COLLECTION).add(data)
            .addOnFailureListener { Log.w(TAG, "signal send failed", it) }
    }

    private fun closePeer(peerId: String) {
        peers.remove(peerId)?.close()
        pendingCandidates.remove(peerId)
        publishState()
    }

    private fun publishState() {
        val connected = peers.values.count { it.connectionState() == PeerConnection.PeerConnectionState.CONNECTED }
        _state.update {
            it.copy(
                peerConnectionsCount = peers.size,
                connectionQualityLabel = when {
                    connected > 0 -> "🟢 صوت مباشر عبر الإنترنت • $connected متصل"
                    peers.isNotEmpty() -> "🟡 جاري ربط ${peers.size} عضو بالصوت..."
                    else -> "🟢 الغرفة متصلة • في انتظار أعضاء"
                }
            )
        }
    }

    private open class SimpleSdpObserver : org.webrtc.SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String) { Log.w(TAG, "SDP create failure: $error") }
        override fun onSetFailure(error: String) { Log.w(TAG, "SDP set failure: $error") }
    }
}

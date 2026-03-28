import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/video_generator_service.dart';

final videoGeneratorServiceProvider = Provider((ref) => VideoGeneratorService());

class VideoGeneratorState {
  final String? jobId;
  final String status;
  final String stage;
  final int progress;
  final String? videoUrl;
  final String? error;

  VideoGeneratorState({
    this.jobId,
    this.status = 'idle',
    this.stage = '',
    this.progress = 0,
    this.videoUrl,
    this.error,
  });

  VideoGeneratorState copyWith({
    String? jobId,
    String? status,
    String? stage,
    int? progress,
    String? videoUrl,
    String? error,
  }) {
    return VideoGeneratorState(
      jobId: jobId ?? this.jobId,
      status: status ?? this.status,
      stage: stage ?? this.stage,
      progress: progress ?? this.progress,
      videoUrl: videoUrl ?? this.videoUrl,
      error: error,
    );
  }
  
  bool get isGenerating => status != 'idle' && status != 'completed' && status != 'failed';
}

class VideoGeneratorNotifier extends StateNotifier<VideoGeneratorState> {
  final VideoGeneratorService _service;
  Timer? _pollingTimer;

  VideoGeneratorNotifier(this._service) : super(VideoGeneratorState());

  Future<void> generateVideo(String prompt, String userId) async {
    state = VideoGeneratorState(status: 'queued', stage: 'initializing', progress: 0);
    
    try {
      final jobId = await _service.startVideoGeneration(prompt, userId);
      state = state.copyWith(jobId: jobId);
      _startPolling(jobId);
    } catch (e) {
      state = state.copyWith(status: 'failed', error: e.toString());
    }
  }

  void _startPolling(String jobId) {
    _pollingTimer?.cancel();
    _pollingTimer = Timer.periodic(const Duration(seconds: 3), (timer) async {
      try {
        final update = await _service.getJobStatus(jobId);
        
        state = state.copyWith(
          status: update.status,
          stage: update.stage,
          progress: update.progress,
          videoUrl: update.videoUrl,
          error: update.error,
        );

        if (update.isCompleted || update.isFailed) {
          timer.cancel();
        }
      } catch (e) {
        print("Polling error: $e");
      }
    });
  }

  @override
  void dispose() {
    _pollingTimer?.cancel();
    super.dispose();
  }
}

final videoGeneratorProvider = StateNotifierProvider<VideoGeneratorNotifier, VideoGeneratorState>((ref) {
  return VideoGeneratorNotifier(ref.watch(videoGeneratorServiceProvider));
});

import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import 'package:chewie/chewie.dart';

class StreamingVideoPlayer extends StatefulWidget {
  final String videoUrl;
  final String? title;

  const StreamingVideoPlayer({
    super.key,
    required this.videoUrl,
    this.title,
  });

  @override
  State<StreamingVideoPlayer> createState() => _StreamingVideoPlayerState();
}

class _StreamingVideoPlayerState extends State<StreamingVideoPlayer> {
  VideoPlayerController? _videoPlayerController;
  ChewieController? _chewieController;
  bool _hasError = false;
  String _errorMessage = "";
  int _retryCount = 0;
  static const int maxRetries = 3;

  @override
  void initState() {
    super.initState();
    _initializePlayer();
  }

  Future<void> _initializePlayer() async {
    setState(() {
      _hasError = false;
      _errorMessage = "";
    });

    try {
      _videoPlayerController = VideoPlayerController.networkUrl(Uri.parse(widget.videoUrl));
      
      await _videoPlayerController!.initialize();

      _chewieController = ChewieController(
        videoPlayerController: _videoPlayerController!,
        autoPlay: false,
        looping: false,
        aspectRatio: _videoPlayerController!.value.aspectRatio,
        placeholder: Container(
          color: Colors.black,
          child: const Center(child: CircularProgressIndicator()),
        ),
        errorBuilder: (context, errorMessage) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.error, color: Colors.white, size: 42),
                const SizedBox(height: 8),
                Text(errorMessage, style: const TextStyle(color: Colors.white)),
              ],
            ),
          );
        },
      );
      setState(() {});
    } catch (e) {
      _handleError(e.toString());
    }
  }

  void _handleError(String error) {
    setState(() {
      _hasError = true;
      _errorMessage = error;
    });
    
    if (_retryCount < maxRetries) {
      _retryCount++;
      Future.delayed(const Duration(seconds: 2), _initializePlayer);
    }
  }

  @override
  void dispose() {
    _videoPlayerController?.dispose();
    _chewieController?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_hasError && _retryCount >= maxRetries) {
      return Container(
        height: 200,
        color: Colors.black87,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.cloud_off, color: Colors.red, size: 48),
            const SizedBox(height: 16),
            const Text("Failed to load video", style: TextStyle(color: Colors.white)),
            TextButton(
              onPressed: () {
                _retryCount = 0;
                _initializePlayer();
              },
              child: const Text("Retry Now"),
            )
          ],
        ),
      );
    }

    return AspectRatio(
      aspectRatio: _chewieController?.aspectRatio ?? 16 / 9,
      child: _chewieController != null && _chewieController!.videoPlayerController.value.isInitialized
          ? Chewie(controller: _chewieController!)
          : Container(
              color: Colors.black,
              child: const Center(child: CircularProgressIndicator()),
            ),
    );
  }
}

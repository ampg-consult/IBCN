import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../viewmodels/video_generator_viewmodel.dart';
import 'widgets/streaming_video_player.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';

class VideoGeneratorScreen extends ConsumerStatefulWidget {
  const VideoGeneratorScreen({super.key});

  @override
  ConsumerState<VideoGeneratorScreen> createState() => _VideoGeneratorScreenState();
}

class _VideoGeneratorScreenState extends ConsumerState<VideoGeneratorScreen> {
  final TextEditingController _promptController = TextEditingController();

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(videoGeneratorProvider);
    final notifier = ref.read(videoGeneratorProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        title: const Text('AI Startup Video Studio'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildPromptInput(state, notifier),
            const SizedBox(height: 24),
            if (state.status != 'idle')
              _buildProgressPanel(state),
            if (state.videoUrl != null && state.status == 'completed')
              _buildVideoWorkspace(state),
          ],
        ),
      ),
    );
  }

  Widget _buildPromptInput(VideoGeneratorState state, VideoGeneratorNotifier notifier) {
    final isGenerating = state.status != 'idle' && state.status != 'completed' && state.status != 'failed';
    
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.auto_awesome, color: Theme.of(context).colorScheme.primary),
                const SizedBox(width: 8),
                const Text(
                  "AI Video Launchpad",
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _promptController,
              decoration: InputDecoration(
                hintText: "Describe your startup promo...",
                filled: true,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
              ),
              maxLines: 3,
              enabled: !isGenerating,
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: isGenerating ? null : () => notifier.generateVideo(_promptController.text, "user_id_here"),
                icon: const Icon(Icons.movie_creation_outlined),
                label: Text(isGenerating ? "Generating Magic..." : "Generate Professional Video"),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildProgressPanel(VideoGeneratorState state) {
    final isDone = state.status == 'completed';
    final isFailed = state.status == 'failed';
    
    return Card(
      color: Theme.of(context).colorScheme.primaryContainer.withOpacity(0.1),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            if (!isDone && !isFailed) ...[
              LinearProgressIndicator(value: state.progress / 100),
              const SizedBox(height: 12),
              Text("${state.stage}: ${state.progress}%", style: const TextStyle(fontWeight: FontWeight.w500)),
            ],
            if (isFailed)
              Text(state.error ?? "Generation failed", style: const TextStyle(color: Colors.red)),
            if (isDone)
              const Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.check_circle, color: Colors.green),
                  SizedBox(width: 8),
                  Text("Generation Complete!", style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildVideoWorkspace(VideoGeneratorState state) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 16),
        const Text("Production Review", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        const SizedBox(height: 12),
        ClipRRect(
          borderRadius: BorderRadius.circular(16),
          child: StreamingVideoPlayer(
            videoUrl: state.videoUrl!,
            title: "Generated Video",
          ),
        ),
        const SizedBox(height: 24),
        _buildViralDistributionRow(),
      ],
    );
  }

  Widget _buildViralDistributionRow() {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          _socialChip("TikTok", FontAwesomeIcons.tiktok, Colors.black),
          _socialChip("YouTube", FontAwesomeIcons.youtube, Colors.red),
          _socialChip("Instagram", FontAwesomeIcons.instagram, Colors.purple),
          _socialChip("LinkedIn", FontAwesomeIcons.linkedin, Colors.blue),
        ],
      ),
    );
  }

  Widget _socialChip(String label, IconData icon, Color color) {
    return Padding(
      padding: const EdgeInsets.only(right: 8.0),
      child: ActionChip(
        avatar: Icon(icon, size: 16, color: color),
        label: Text(label),
        onPressed: () {},
      ),
    );
  }
}

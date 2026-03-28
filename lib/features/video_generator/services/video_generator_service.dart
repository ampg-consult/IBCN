import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;

class VideoGeneratorService {
  // PRODUCTION URL: Pointing to your Railway instance
  final String baseUrl = "https://ibcn-production.up.railway.app"; 

  Future<String> startVideoGeneration(String prompt, String userId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/generate-video'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'prompt': prompt,
        'userId': userId,
      }),
    );

    if (response.statusCode == 202) {
      final data = jsonDecode(response.body);
      return data['jobId'];
    } else {
      throw Exception('Failed to start video generation: ${response.body}');
    }
  }

  Future<JobStatusUpdate> getJobStatus(String jobId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/status/$jobId'),
      headers: {'Cache-Control': 'no-cache'},
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return JobStatusUpdate.fromJson(data);
    } else if (response.statusCode == 404) {
      // Handle early polling before Redis persists
      return JobStatusUpdate(status: 'pending', stage: 'script', progress: 0);
    } else {
      throw Exception('Failed to get job status: ${response.code}');
    }
  }
}

class JobStatusUpdate {
  final String status;
  final String stage;
  final int progress;
  final String? videoUrl;
  final String? error;

  JobStatusUpdate({
    required this.status,
    required this.stage,
    required this.progress,
    this.videoUrl,
    this.error,
  });

  factory JobStatusUpdate.fromJson(Map<String, dynamic> json) {
    return JobStatusUpdate(
      status: json['status'] ?? 'unknown',
      stage: json['stage'] ?? '',
      progress: (json['progress'] ?? 0).toInt(),
      videoUrl: json['videoUrl'],
      error: json['error'],
    );
  }

  // TERMINAL STATE CHECK: Matching backend 'completed' standard
  bool get isCompleted => status == 'completed';
  bool get isFailed => status == 'failed';
}

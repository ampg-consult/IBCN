import 'package:cloud_firestore/cloud_firestore.dart';

enum VideoGenerationStatus { idle, scripting, generatingScenes, addingVoiceover, rendering, completed, failed }

class VideoAsset {
  final String id;
  final String creatorId;
  final String title;
  final String description;
  final String videoUrl;
  final String thumbnailUrl;
  final List<String> tags;
  final double suggestedPrice;
  final double rating;
  final int downloadCount;
  final DateTime createdAt;
  final Map<String, dynamic>? metadata; // Storyboard, script, etc.

  VideoAsset({
    required this.id,
    required this.creatorId,
    required this.title,
    required this.description,
    required this.videoUrl,
    required this.thumbnailUrl,
    this.tags = const [],
    this.suggestedPrice = 0.0,
    this.rating = 0.0,
    this.downloadCount = 0,
    required this.createdAt,
    this.metadata,
  });

  factory VideoAsset.fromFirestore(DocumentSnapshot doc) {
    Map<String, dynamic> data = doc.data() as Map<String, dynamic>;
    return VideoAsset(
      id: doc.id,
      creatorId: data['creatorId'] ?? '',
      title: data['title'] ?? '',
      description: data['description'] ?? '',
      videoUrl: data['videoUrl'] ?? '',
      thumbnailUrl: data['thumbnailUrl'] ?? '',
      tags: List<String>.from(data['tags'] ?? []),
      suggestedPrice: (data['suggestedPrice'] ?? 0.0).toDouble(),
      rating: (data['rating'] ?? 0.0).toDouble(),
      downloadCount: data['downloadCount'] ?? 0,
      createdAt: (data['createdAt'] as Timestamp).toDate(),
      metadata: data['metadata'],
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'creatorId': creatorId,
      'title': title,
      'description': description,
      'videoUrl': videoUrl,
      'thumbnailUrl': thumbnailUrl,
      'tags': tags,
      'suggestedPrice': suggestedPrice,
      'rating': rating,
      'downloadCount': downloadCount,
      'createdAt': Timestamp.fromDate(createdAt),
      'metadata': metadata,
    };
  }
}

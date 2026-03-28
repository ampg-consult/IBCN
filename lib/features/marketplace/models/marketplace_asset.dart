import 'package:cloud_firestore/cloud_firestore.dart';

enum AssetType { template, agent, workflow, uiKit, devTool }

class MarketplaceAsset {
  final String id;
  final String creatorId;
  final String projectId;
  final String title;
  final String description;
  final AssetType type;
  final double price;
  final double rating;
  final int downloadCount;
  final List<String> tags;
  final List<String> imageUrls;
  final DateTime createdAt;
  final DateTime updatedAt;

  MarketplaceAsset({
    required this.id,
    required this.creatorId,
    required this.projectId,
    required this.title,
    required this.description,
    required this.type,
    this.price = 0.0,
    this.rating = 0.0,
    this.downloadCount = 0,
    this.tags = const [],
    this.imageUrls = const [],
    required this.createdAt,
    required this.updatedAt,
  });

  factory MarketplaceAsset.fromFirestore(DocumentSnapshot doc) {
    Map<String, dynamic> data = doc.data() as Map<String, dynamic>;
    return MarketplaceAsset(
      id: doc.id,
      creatorId: data['creatorId'] ?? '',
      projectId: data['projectId'] ?? '',
      title: data['title'] ?? '',
      description: data['description'] ?? '',
      type: AssetType.values.firstWhere(
        (e) => e.toString().split('.').last == (data['type'] ?? 'template'),
        orElse: () => AssetType.template,
      ),
      price: (data['price'] ?? 0.0).toDouble(),
      rating: (data['rating'] ?? 0.0).toDouble(),
      downloadCount: data['downloadCount'] ?? 0,
      tags: List<String>.from(data['tags'] ?? []),
      imageUrls: List<String>.from(data['imageUrls'] ?? []),
      createdAt: (data['createdAt'] as Timestamp).toDate(),
      updatedAt: (data['updatedAt'] as Timestamp).toDate(),
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'creatorId': creatorId,
      'projectId': projectId,
      'title': title,
      'description': description,
      'type': type.toString().split('.').last,
      'price': price,
      'rating': rating,
      'downloadCount': downloadCount,
      'tags': tags,
      'imageUrls': imageUrls,
      'createdAt': Timestamp.fromDate(createdAt),
      'updatedAt': Timestamp.fromDate(updatedAt),
    };
  }
}

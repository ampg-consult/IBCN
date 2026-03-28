import 'package:cloud_firestore/cloud_firestore.dart';

enum ProjectStatus { idea, building, deployed, monetized }

class Project {
  final String id;
  final String ownerUid;
  final String name;
  final String description;
  final double progress;
  final ProjectStatus status;
  final List<String> tags;
  final DateTime createdAt;
  final DateTime updatedAt;
  
  // SaaS Specific Fields
  final bool isSaaS;
  final String? stripeProductId;
  final Map<String, double>? pricingTiers; // e.g. {'Pro': 19.99, 'Premium': 49.99}

  Project({
    required this.id,
    required this.ownerUid,
    required this.name,
    required this.description,
    this.progress = 0.0,
    this.status = ProjectStatus.idea,
    this.tags = const [],
    required this.createdAt,
    required this.updatedAt,
    this.isSaaS = false,
    this.stripeProductId,
    this.pricingTiers,
  });

  factory Project.fromFirestore(DocumentSnapshot doc) {
    Map<String, dynamic> data = doc.data() as Map<String, dynamic>;
    return Project(
      id: doc.id,
      ownerUid: data['ownerUid'] ?? '',
      name: data['name'] ?? 'Untitled Project',
      description: data['description'] ?? '',
      progress: (data['progress'] ?? 0.0).toDouble(),
      status: ProjectStatus.values.firstWhere(
        (e) => e.toString().split('.').last == (data['status'] ?? 'idea'),
        orElse: () => ProjectStatus.idea,
      ),
      tags: List<String>.from(data['tags'] ?? []),
      createdAt: (data['createdAt'] as Timestamp).toDate(),
      updatedAt: (data['updatedAt'] as Timestamp).toDate(),
      isSaaS: data['isSaaS'] ?? false,
      stripeProductId: data['stripeProductId'],
      pricingTiers: data['pricingTiers'] != null 
          ? Map<String, double>.from(data['pricingTiers']) 
          : null,
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'ownerUid': ownerUid,
      'name': name,
      'description': description,
      'progress': progress,
      'status': status.toString().split('.').last,
      'tags': tags,
      'createdAt': Timestamp.fromDate(createdAt),
      'updatedAt': Timestamp.fromDate(updatedAt),
      'isSaaS': isSaaS,
      'stripeProductId': stripeProductId,
      'pricingTiers': pricingTiers,
    };
  }
}

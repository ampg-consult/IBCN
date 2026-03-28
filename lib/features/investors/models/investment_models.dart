import 'package:cloud_firestore/cloud_firestore.dart';

enum EscrowStatus { pending, funded, milestoneReached, released, disputed, refunded }

class InvestmentBounty {
  final String id;
  final String investorId;
  final String projectId;
  final double amount;
  final double equityOffered; // In percentage
  final EscrowStatus status;
  final List<Milestone> milestones;
  final DateTime createdAt;

  InvestmentBounty({
    required this.id,
    required this.investorId,
    required this.projectId,
    required this.amount,
    required this.equityOffered,
    this.status = EscrowStatus.pending,
    this.milestones = const [],
    required this.createdAt,
  });

  factory InvestmentBounty.fromFirestore(DocumentSnapshot doc) {
    final data = doc.data() as Map<String, dynamic>;
    return InvestmentBounty(
      id: doc.id,
      investorId: data['investorId'] ?? '',
      projectId: data['projectId'] ?? '',
      amount: (data['amount'] ?? 0.0).toDouble(),
      equityOffered: (data['equityOffered'] ?? 0.0).toDouble(),
      status: EscrowStatus.values.firstWhere(
        (e) => e.name == (data['status'] ?? 'pending'),
        orElse: () => EscrowStatus.pending,
      ),
      milestones: (data['milestones'] as List? ?? [])
          .map((m) => Milestone.fromMap(m))
          .toList(),
      createdAt: (data['createdAt'] as Timestamp).toDate(),
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'investorId': investorId,
      'projectId': projectId,
      'amount': amount,
      'equityOffered': equityOffered,
      'status': status.name,
      'milestones': milestones.map((m) => m.toMap()).toList(),
      'createdAt': Timestamp.fromDate(createdAt),
    };
  }
}

class Milestone {
  final String title;
  final String description;
  final bool isCompleted;
  final double releaseAmount;

  Milestone({
    required this.title,
    required this.description,
    this.isCompleted = false,
    required this.releaseAmount,
  });

  factory Milestone.fromMap(Map<String, dynamic> map) {
    return Milestone(
      title: map['title'] ?? '',
      description: map['description'] ?? '',
      isCompleted: map['isCompleted'] ?? false,
      releaseAmount: (map['releaseAmount'] ?? 0.0).toDouble(),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'title': title,
      'description': description,
      'isCompleted': isCompleted,
      'releaseAmount': releaseAmount,
    };
  }
}

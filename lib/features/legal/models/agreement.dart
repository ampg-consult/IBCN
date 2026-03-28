import 'package:cloud_firestore/cloud_firestore.dart';

enum AgreementType { nda, investment, coFounder, serviceContract }
enum AgreementStatus { draft, pendingSignature, fullySigned, cancelled }

class Agreement {
  final String id;
  final String projectId;
  final String title;
  final String content;
  final AgreementType type;
  final AgreementStatus status;
  final List<String> partyUids; // List of user IDs involved
  final Map<String, dynamic> signatures; // uid -> {timestamp, ip, name}
  final DateTime createdAt;
  final String disclaimer;

  Agreement({
    required this.id,
    required this.projectId,
    required this.title,
    required this.content,
    required this.type,
    this.status = AgreementStatus.draft,
    required this.partyUids,
    this.signatures = const {},
    required this.createdAt,
    this.disclaimer = "This document is AI-generated and should be reviewed by a qualified legal professional.",
  });

  factory Agreement.fromFirestore(DocumentSnapshot doc) {
    final data = doc.data() as Map<String, dynamic>;
    return Agreement(
      id: doc.id,
      projectId: data['projectId'] ?? '',
      title: data['title'] ?? '',
      content: data['content'] ?? '',
      type: AgreementType.values.firstWhere(
        (e) => e.name == (data['type'] ?? 'nda'),
        orElse: () => AgreementType.nda,
      ),
      status: AgreementStatus.values.firstWhere(
        (e) => e.name == (data['status'] ?? 'draft'),
        orElse: () => AgreementStatus.draft,
      ),
      partyUids: List<String>.from(data['partyUids'] ?? []),
      signatures: Map<String, dynamic>.from(data['signatures'] ?? {}),
      createdAt: (data['createdAt'] as Timestamp).toDate(),
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'projectId': projectId,
      'title': title,
      'content': content,
      'type': type.name,
      'status': status.name,
      'partyUids': partyUids,
      'signatures': signatures,
      'createdAt': Timestamp.fromDate(createdAt),
      'disclaimer': disclaimer,
    };
  }
}

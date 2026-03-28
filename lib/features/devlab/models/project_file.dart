import 'package:cloud_firestore/cloud_firestore.dart';

class ProjectFile {
  final String id;
  final String projectId;
  final String fileName;
  final String filePath;
  final String content;
  final String language;
  final DateTime createdAt;
  final DateTime updatedAt;

  ProjectFile({
    required this.id,
    required this.projectId,
    required this.fileName,
    required this.filePath,
    required this.content,
    required this.language,
    required this.createdAt,
    required this.updatedAt,
  });

  factory ProjectFile.fromFirestore(DocumentSnapshot doc) {
    Map<String, dynamic> data = doc.data() as Map<String, dynamic>;
    return ProjectFile(
      id: doc.id,
      projectId: data['projectId'] ?? '',
      fileName: data['fileName'] ?? '',
      filePath: data['filePath'] ?? '',
      content: data['content'] ?? '',
      language: data['language'] ?? 'dart',
      createdAt: (data['createdAt'] as Timestamp).toDate(),
      updatedAt: (data['updatedAt'] as Timestamp).toDate(),
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'projectId': projectId,
      'fileName': fileName,
      'filePath': filePath,
      'content': content,
      'language': language,
      'createdAt': Timestamp.fromDate(createdAt),
      'updatedAt': Timestamp.fromDate(updatedAt),
    };
  }

  ProjectFile copyWith({String? content}) {
    return ProjectFile(
      id: id,
      projectId: projectId,
      fileName: fileName,
      filePath: filePath,
      content: content ?? this.content,
      language: language,
      createdAt: createdAt,
      updatedAt: DateTime.now(),
    );
  }
}

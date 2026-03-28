import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/project_file.dart';

class DevLabRepository {
  final FirebaseFirestore _firestore;

  DevLabRepository(this._firestore);

  Stream<List<ProjectFile>> watchProjectFiles(String projectId) {
    return _firestore
        .collection('projects')
        .doc(projectId)
        .collection('files')
        .orderBy('filePath')
        .snapshots()
        .map((snapshot) => snapshot.docs
            .map((doc) => ProjectFile.fromFirestore(doc))
            .toList());
  }

  Future<void> updateFileContent(String projectId, String fileId, String content) async {
    await _firestore
        .collection('projects')
        .doc(projectId)
        .collection('files')
        .doc(fileId)
        .update({
      'content': content,
      'updatedAt': FieldValue.serverTimestamp(),
    });
  }

  Future<void> createFile(String projectId, ProjectFile file) async {
    await _firestore
        .collection('projects')
        .doc(projectId)
        .collection('files')
        .add(file.toFirestore());
  }
}

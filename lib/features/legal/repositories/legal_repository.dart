import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/agreement.dart';

class LegalRepository {
  final FirebaseFirestore _firestore;

  LegalRepository(this._firestore);

  Stream<List<Agreement>> watchProjectAgreements(String projectId) {
    return _firestore
        .collection('agreements')
        .where('projectId', isEqualTo: projectId)
        .snapshots()
        .map((snapshot) => snapshot.docs
            .map((doc) => Agreement.fromFirestore(doc))
            .toList());
  }

  Future<void> createAgreement(Agreement agreement) async {
    await _firestore.collection('agreements').doc(agreement.id).set(agreement.toFirestore());
  }

  Future<void> signAgreement(String agreementId, String userUid, String fullName) async {
    await _firestore.collection('agreements').doc(agreementId).update({
      'signatures.$userUid': {
        'timestamp': FieldValue.serverTimestamp(),
        'name': fullName,
        'status': 'SIGNED',
      },
      'status': AgreementStatus.pendingSignature.name, // Logic to update to fullySigned if all parties signed
    });
  }
}

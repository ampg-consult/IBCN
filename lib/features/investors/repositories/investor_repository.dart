import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/investment_models.dart';
import '../../projects/models/project.dart';

class InvestorRepository {
  final FirebaseFirestore _firestore;

  InvestorRepository(this._firestore);

  // Stream of projects that are looking for investment
  Stream<List<Project>> watchInvestableProjects() {
    return _firestore
        .collection('projects')
        .where('status', isEqualTo: 'deployed') // Only deployed projects can seek funding
        .snapshots()
        .map((snapshot) => snapshot.docs
            .map((doc) => Project.fromFirestore(doc))
            .toList());
  }

  // Create a new investment bounty (Escrow)
  Future<void> createInvestment(InvestmentBounty investment) async {
    final docRef = _firestore.collection('investments').doc();
    await docRef.set(investment.toFirestore());
    
    // Update project status to indicate funding in progress
    await _firestore.collection('projects').doc(investment.projectId).update({
      'status': 'funding',
    });
  }

  // Release funds for a milestone
  Future<void> releaseMilestone(String investmentId, int milestoneIndex) async {
    final docRef = _firestore.collection('investments').doc(investmentId);
    
    await _firestore.runTransaction((transaction) async {
      final snapshot = await transaction.get(docRef);
      if (!snapshot.exists) throw Exception("Investment not found");
      
      final data = snapshot.data() as Map<String, dynamic>;
      final milestones = List<Map<String, dynamic>>.from(data['milestones']);
      
      if (milestoneIndex >= milestones.length) throw Exception("Invalid milestone index");
      
      milestones[milestoneIndex]['isCompleted'] = true;
      
      transaction.update(docRef, {'milestones': milestones});
      
      // logic to actually transfer funds via Stripe Connect would happen in a Cloud Function
    });
  }

  Stream<List<InvestmentBounty>> watchInvestorPortfolio(String investorId) {
    return _firestore
        .collection('investments')
        .where('investorId', isEqualTo: investorId)
        .snapshots()
        .map((snapshot) => snapshot.docs
            .map((doc) => InvestmentBounty.fromFirestore(doc))
            .toList());
  }
}

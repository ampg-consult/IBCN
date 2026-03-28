import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/marketplace_asset.dart';

class MarketplaceRepository {
  final FirebaseFirestore _firestore;

  MarketplaceRepository(this._firestore);

  Stream<List<MarketplaceAsset>> watchAssets({String? category}) {
    Query query = _firestore.collection('marketplace_assets');
    
    if (category != null && category != 'All') {
      // AssetType enum names are lowercase in Firestore
      query = query.where('type', isEqualTo: category.toLowerCase().replaceAll(' ', ''));
    }

    return query.snapshots().map((snapshot) => snapshot.docs
        .map((doc) => MarketplaceAsset.fromFirestore(doc))
        .toList());
  }

  Future<void> publishAsset(MarketplaceAsset asset) async {
    await _firestore.collection('marketplace_assets').add(asset.toFirestore());
  }

  Future<void> incrementDownloadCount(String assetId) async {
    await _firestore.collection('marketplace_assets').doc(assetId).update({
      'downloadCount': FieldValue.increment(1),
    });
  }
}

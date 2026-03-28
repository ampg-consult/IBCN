import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import '../services/saas_service.dart';

final saasServiceProvider = Provider((ref) => SaaSService(FirebaseFirestore.instance));

class SaaSState {
  final bool isProcessing;
  final Map<String, dynamic>? analytics;
  final String? error;

  SaaSState({this.isProcessing = false, this.analytics, this.error});

  SaaSState copyWith({bool? isProcessing, Map<String, dynamic>? analytics, String? error}) {
    return SaaSState(
      isProcessing: isProcessing ?? this.isProcessing,
      analytics: analytics ?? this.analytics,
      error: error ?? this.error,
    );
  }
}

class SaaSNotifier extends StateNotifier<SaaSState> {
  final SaaSService _service;

  SaaSNotifier(this._service) : super(SaaSState());

  Future<void> launchSaaS(String projectId, Map<String, double> tiers) async {
    state = state.copyWith(isProcessing: true, error: null);
    try {
      await _service.convertToSaaS(projectId, tiers);
      await loadAnalytics(projectId);
    } catch (e) {
      state = state.copyWith(error: e.toString());
    } finally {
      state = state.copyWith(isProcessing: false);
    }
  }

  Future<void> loadAnalytics(String projectId) async {
    final data = await _service.getSaaSAnalytics(projectId);
    state = state.copyWith(analytics: data);
  }
}

final saasProvider = StateNotifierProvider<SaaSNotifier, SaaSState>((ref) {
  return SaaSNotifier(ref.watch(saasServiceProvider));
});

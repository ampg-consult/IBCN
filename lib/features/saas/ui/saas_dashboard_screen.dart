import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../viewmodels/saas_viewmodel.dart';
import '../../projects/models/project.dart';

class SaaSDashboardScreen extends ConsumerStatefulWidget {
  final Project project;
  const SaaSDashboardScreen({super.key, required this.project});

  @override
  ConsumerState<SaaSDashboardScreen> createState() => _SaaSDashboardScreenState();
}

class _SaaSDashboardScreenState extends ConsumerState<SaaSDashboardScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => 
      ref.read(saasProvider.notifier).loadAnalytics(widget.project.id)
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(saasProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.project.name} - SaaS Admin'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildTierSummary(),
            const SizedBox(height: 24),
            Text('Business Performance', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            if (state.analytics != null)
              _AnalyticsGrid(data: state.analytics!)
            else
              const Center(child: CircularProgressIndicator()),
            const SizedBox(height: 32),
            _SubscriberList(projectId: widget.project.id),
          ],
        ),
      ),
    );
  }

  Widget _buildTierSummary() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('Monetization Status'),
                Chip(
                  label: const Text('ACTIVE'),
                  backgroundColor: Colors.green.withOpacity(0.2),
                  labelStyle: const TextStyle(color: Colors.green),
                ),
              ],
            ),
            const Divider(height: 32),
            ...?widget.project.pricingTiers?.entries.map((e) => Padding(
              padding: const EdgeInsets.symmetric(vertical: 4.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(e.key),
                  Text('\$${e.value.toStringAsFixed(2)} / mo', style: const TextStyle(fontWeight: FontWeight.bold)),
                ],
              ),
            )),
          ],
        ),
      ),
    );
  }
}

class _AnalyticsGrid extends StatelessWidget {
  final Map<String, dynamic> data;
  const _AnalyticsGrid({required this.data});

  @override
  Widget build(BuildContext context) {
    return GridView.count(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisCount: 2,
      childAspectRatio: 1.5,
      crossAxisSpacing: 16,
      mainAxisSpacing: 16,
      children: [
        _MetricCard(label: 'MRR', value: '\$${data['mrr']}', icon: Icons.monetization_on),
        _MetricCard(label: 'Active Subs', value: '${data['activeSubscribers']}', icon: Icons.people),
        _MetricCard(label: 'Churn', value: '${data['churnRate']}%', icon: Icons.trending_down),
        _MetricCard(label: 'Conv. Rate', value: '${data['conversionRate']}%', icon: Icons.shopping_cart),
      ],
    );
  }
}

class _MetricCard extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  const _MetricCard({required this.label, required this.value, required this.icon});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 20, color: Theme.of(context).colorScheme.primary),
            const SizedBox(height: 4),
            Text(value, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            Text(label, style: const TextStyle(fontSize: 10, color: Colors.grey)),
          ],
        ),
      ),
    );
  }
}

class _SubscriberList extends ConsumerWidget {
  final String projectId;
  const _SubscriberList({required this.projectId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final stream = ref.watch(saasServiceProvider).watchSubscribers(projectId);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Recent Subscribers', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 12),
        StreamBuilder(
          stream: stream,
          builder: (context, snapshot) {
            if (!snapshot.hasData) return const LinearProgressIndicator();
            final docs = snapshot.data!.docs;
            if (docs.isEmpty) return const Text('No active subscribers yet.');
            
            return ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: docs.length,
              itemBuilder: (context, index) {
                final sub = docs[index].data() as Map<String, dynamic>;
                return ListTile(
                  leading: const CircleAvatar(child: Icon(Icons.person)),
                  title: Text(sub['email'] ?? 'User'),
                  subtitle: Text('Plan: ${sub['tier']}'),
                  trailing: Text(sub['status'] ?? 'Active', style: const TextStyle(color: Colors.green)),
                );
              },
            );
          },
        ),
      ],
    );
  }
}

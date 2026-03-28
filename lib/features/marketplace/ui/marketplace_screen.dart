import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:responsive_framework/responsive_framework.dart';

class MarketplaceScreen extends StatefulWidget {
  const MarketplaceScreen({super.key});

  @override
  State<MarketplaceScreen> createState() => _MarketplaceScreenState();
}

class _MarketplaceScreenState extends State<MarketplaceScreen> {
  String selectedCategory = 'All';
  final categories = ['All', 'Templates', 'AI Agents', 'Workflows', 'UI Kits', 'Dev Tools'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('IBCN Marketplace'),
        actions: [
          IconButton(
            icon: const Icon(Icons.video_call_outlined, color: Colors.purpleAccent),
            tooltip: "Generate Video Ad",
            onPressed: () => context.push('/video-studio'),
          ),
          IconButton(icon: const Icon(Icons.search), onPressed: () {}),
          IconButton(
            icon: Badge(
              label: const Text('0'),
              child: const Icon(Icons.shopping_cart_outlined),
            ),
            onPressed: () {},
          ),
        ],
      ),
      body: Column(
        children: [
          _CategorySelector(
            categories: categories,
            selectedCategory: selectedCategory,
            onCategorySelected: (val) => setState(() => selectedCategory = val),
          ),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                const _FeaturedAsset(),
                const SizedBox(height: 24),
                Text(
                  'Available Assets',
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 16),
                const _AssetGrid(),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _CategorySelector extends StatelessWidget {
  final List<String> categories;
  final String selectedCategory;
  final Function(String) onCategorySelected;

  const _CategorySelector({
    required this.categories,
    required this.selectedCategory,
    required this.onCategorySelected,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 60,
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        scrollDirection: Axis.horizontal,
        itemCount: categories.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final category = categories[index];
          final isSelected = selectedCategory == category;
          return ChoiceChip(
            label: Text(category),
            selected: isSelected,
            onSelected: (_) => onCategorySelected(category),
          );
        },
      ),
    );
  }
}

class _FeaturedAsset extends StatelessWidget {
  const _FeaturedAsset();

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 180,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Theme.of(context).colorScheme.primary, Theme.of(context).colorScheme.tertiaryContainer],
        ),
        borderRadius: BorderRadius.circular(24),
      ),
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.2),
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Text('NEW RELEASE', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
          ),
          const SizedBox(height: 12),
          Text(
            'Autonomous AI DevOps',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold, color: Colors.white),
          ),
          Text(
            'Deploy entire stacks with one prompt.',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: Colors.white70),
          ),
        ],
      ),
    );
  }
}

class _AssetGrid extends StatelessWidget {
  const _AssetGrid();

  @override
  Widget build(BuildContext context) {
    final bool isMobile = ResponsiveBreakpoints.of(context).isMobile;
    final int crossAxisCount = isMobile ? 2 : (ResponsiveBreakpoints.of(context).isTablet ? 3 : 4);

    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: crossAxisCount,
        crossAxisSpacing: 16,
        mainAxisSpacing: 16,
        childAspectRatio: 0.8,
      ),
      itemCount: 6,
      itemBuilder: (context, index) {
        return const _AssetCard();
      },
    );
  }
}

class _AssetCard extends StatelessWidget {
  const _AssetCard();

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Container(
                width: double.infinity,
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.primary.withOpacity(0.05),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: const Icon(Icons.smart_toy_outlined, size: 40),
              ),
            ),
            const SizedBox(height: 12),
            const Text('AI Architect Pro', style: TextStyle(fontWeight: FontWeight.bold), maxLines: 1),
            Row(
              children: [
                const Icon(Icons.star, color: Colors.amber, size: 14),
                const SizedBox(width: 4),
                Text('4.9', style: Theme.of(context).textTheme.labelSmall),
                const Spacer(),
                Text('Templates', style: Theme.of(context).textTheme.labelSmall?.copyWith(color: Colors.grey)),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('\$49.00', style: TextStyle(fontWeight: FontWeight.bold, color: Theme.of(context).colorScheme.primary)),
                IconButton.filledTonal(
                  iconSize: 18,
                  onPressed: () {},
                  icon: const Icon(Icons.add),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

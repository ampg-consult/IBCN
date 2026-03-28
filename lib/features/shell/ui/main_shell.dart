import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:responsive_framework/responsive_framework.dart';

class MainShell extends StatelessWidget {
  final Widget child;
  const MainShell({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    final bool isMobile = ResponsiveBreakpoints.of(context).isMobile;

    return Scaffold(
      body: isMobile
          ? child
          : Row(
              children: [
                const _SidebarNavigation(),
                const VerticalDivider(width: 1, thickness: 1),
                Expanded(child: child),
              ],
            ),
      bottomNavigationBar: isMobile ? const _BottomNavigation() : null,
    );
  }
}

class _SidebarNavigation extends StatelessWidget {
  const _SidebarNavigation();

  @override
  Widget build(BuildContext context) {
    final state = GoRouterState.of(context);
    final location = state.uri.path;

    return NavigationRail(
      extended: ResponsiveBreakpoints.of(context).isDesktop,
      selectedIndex: _getSelectedIndex(location),
      onDestinationSelected: (index) => _onItemTapped(context, index),
      leading: Padding(
        padding: const EdgeInsets.symmetric(vertical: 24.0),
        child: Text(
          'IBCN',
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.bold,
                letterSpacing: 2,
              ),
        ),
      ),
      destinations: const [
        NavigationRailDestination(
          icon: Icon(Icons.dashboard_outlined),
          selectedIcon: Icon(Icons.dashboard),
          label: Text('Dashboard'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.code_outlined),
          selectedIcon: Icon(Icons.code),
          label: Text('Dev Lab'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.storefront_outlined),
          selectedIcon: Icon(Icons.storefront),
          label: Text('Marketplace'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.monetization_on_outlined),
          selectedIcon: Icon(Icons.monetization_on),
          label: Text('Investors'),
        ),
      ],
    );
  }
}

class _BottomNavigation extends StatelessWidget {
  const _BottomNavigation();

  @override
  Widget build(BuildContext context) {
    final state = GoRouterState.of(context);
    final location = state.uri.path;

    return NavigationBar(
      selectedIndex: _getSelectedIndex(location),
      onDestinationSelected: (index) => _onItemTapped(context, index),
      destinations: const [
        NavigationRequest(
          icon: Icon(Icons.dashboard_outlined),
          selectedIcon: Icon(Icons.dashboard),
          label: 'Home',
        ),
        NavigationRequest(
          icon: Icon(Icons.code_outlined),
          selectedIcon: Icon(Icons.code),
          label: 'Dev Lab',
        ),
        NavigationRequest(
          icon: Icon(Icons.storefront_outlined),
          selectedIcon: Icon(Icons.storefront),
          label: 'Market',
        ),
        NavigationRequest(
          icon: Icon(Icons.monetization_on_outlined),
          selectedIcon: Icon(Icons.monetization_on),
          label: 'Invest',
        ),
      ],
    );
  }
}

// Utility mixin or helper for navigation logic
int _getSelectedIndex(String location) {
  if (location.startsWith('/dashboard')) return 0;
  if (location.startsWith('/devlab')) return 1;
  if (location.startsWith('/marketplace')) return 2;
  if (location.startsWith('/investors')) return 3;
  return 0;
}

void _onItemTapped(BuildContext context, int index) {
  switch (index) {
    case 0:
      context.go('/dashboard');
      break;
    case 1:
      context.go('/devlab');
      break;
    case 2:
      context.go('/marketplace');
      break;
    case 3:
      context.go('/investors');
      break;
  }
}

// Custom destination class to support NavigationBar API
class NavigationRequest extends NavigationDestination {
  const NavigationRequest({
    required super.icon,
    required super.label,
    super.selectedIcon,
    super.key,
  });
}

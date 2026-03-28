import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

// Import Screens
import '../../features/dashboard/ui/dashboard_screen.dart';
import '../../features/auth/ui/auth_screen.dart';
import '../../features/devlab/ui/devlab_screen.dart';
import '../../features/marketplace/ui/marketplace_screen.dart';
import '../../features/investors/ui/investor_marketplace_screen.dart';
import '../../features/video_generator/ui/video_generator_screen.dart';
import '../../features/shell/ui/main_shell.dart';

final goRouterProvier = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/dashboard',
    routes: [
      GoRoute(
        path: '/auth',
        builder: (context, state) => const AuthScreen(),
      ),
      GoRoute(
        path: '/video-studio',
        builder: (context, state) => const VideoGeneratorScreen(),
      ),
      ShellRoute(
        builder: (context, state, child) => MainShell(child: child),
        routes: [
          GoRoute(
            path: '/dashboard',
            builder: (context, state) => const DashboardScreen(),
          ),
          GoRoute(
            path: '/devlab',
            builder: (context, state) => const DevLabScreen(projectId: 'default'),
          ),
          GoRoute(
            path: '/marketplace',
            builder: (context, state) => const MarketplaceScreen(),
          ),
          GoRoute(
            path: '/investors',
            builder: (context, state) => const InvestorMarketplaceScreen(),
          ),
        ],
      ),
    ],
  );
});

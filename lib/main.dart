import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:responsive_framework/responsive_framework.dart';
import 'package:google_fonts/google_fonts.dart';

// Import features (to be created)
import 'core/routing/app_router.dart';
import 'core/theme/app_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize Firebase (Requires google-services.json for Android and Firebase config for Web)
  // For now, wrapping in try-catch to allow UI development without crash
  try {
    await Firebase.initializeApp();
  } catch (e) {
    debugPrint("Firebase initialization skipped or failed: $e");
  }

  runApp(const ProviderScope(child: IBCNApp()));
}

class IBCNApp extends ConsumerWidget {
  const IBCNApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(goRouterProvier);

    return MaterialApp.router(
      title: 'IBCN - Intelligent Builder Collaboration Network',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme, // IBCN uses a professional dark-themed builder UI
      builder: (context, child) => ResponsiveBreakpoints.builder(
        child: child!,
        breakpoints: [
          const Breakpoint(start: 0, end: 600, name: MOBILE),
          const Breakpoint(start: 601, end: 1024, name: TABLET),
          const Breakpoint(start: 1025, end: 1920, name: DESKTOP),
          const Breakpoint(start: 1921, end: double.infinity, name: '4K'),
        ],
      ),
      routerConfig: router,
    );
  }
}

// ReadFlow 主应用入口
import SwiftUI
import UIKit

@main
struct ReadFlowApp: App {
    // 应用状态容器
    @StateObject private var appState = AppState()
    @StateObject private var documentManager = DocumentManager()
    @StateObject private var aiService = AIService()
    @StateObject private var readingProgressManager = ReadingProgressManager()

    init() {
        // 配置全局外观
        configureAppearance()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .environmentObject(documentManager)
                .environmentObject(aiService)
                .environmentObject(readingProgressManager)
                .onAppear {
                    // 初始化服务
                    Task {
                        await readingProgressManager.restoreRecentProgress()
                    }
                }
        }
    }

    private func configureAppearance() {
        // 设置全局 UI 外观
        let appearance = UINavigationBarAppearance()
        appearance.configureWithDefaultBackground()
        appearance.titleTextAttributes = [.foregroundColor: UIColor.label]
        appearance.largeTitleTextAttributes = [.foregroundColor: UIColor.label]

        UINavigationBar.appearance().standardAppearance = appearance
        UINavigationBar.appearance().scrollEdgeAppearance = appearance
        UINavigationBar.appearance().compactAppearance = appearance

        // 设置工具栏外观
        let toolbarAppearance = UIToolbarAppearance()
        toolbarAppearance.configureWithDefaultBackground()

        UIToolbar.appearance().standardAppearance = toolbarAppearance
        UIToolbar.appearance().compactAppearance = toolbarAppearance
    }
}

// 全局应用状态
class AppState: ObservableObject {
    @Published var isLoggedIn = false
    @Published var currentUser: User?
    @Published var isLoading = false
    @Published var errorMessage: String?

    // 主题设置
    @Published var colorScheme: ColorScheme = .light
    @Published var accentColor: Color = .blue

    // iPad 特定状态
    @Published var isSplitView = false
    @Published var sidebarVisibility: NavigationSplitViewVisibility = .all

    // Apple Pencil 状态
    @Published var isPencilModeActive = false
}

// 用户模型
struct User: Identifiable, Codable {
    let id: String
    let email: String
    let displayName: String?
}

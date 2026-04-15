import SwiftUI

// 主内容视图 - 根据设备类型显示不同布局
struct ContentView: View {
    @EnvironmentObject var appState: AppState
    @EnvironmentObject var documentManager: DocumentManager

    var body: some View {
        Group {
            #if os(iOS)
            if appState.isSplitView && UIDevice.current.userInterfaceIdiom == .pad {
                // iPad 多栏布局
                NavigationSplitView(columnVisibility: $appState.sidebarVisibility) {
                    LibrarySidebar()
                        .environmentObject(documentManager)
                } content: {
                    DocumentList()
                        .environmentObject(documentManager)
                } detail: {
                    ReadingView()
                        .environmentObject(documentManager)
                        .environmentObject(appState)
                }
            } else {
                // iPhone 单栏布局
                MainTabView()
            }
            #else
            Text("macOS 版本待开发")
            #endif
        }
    }
}

// iPhone 主标签视图
struct MainTabView: View {
    @State private var selection = 0

    var body: some View {
        TabView(selection: $selection) {
            LibraryView()
                .tabItem {
                    Label("Library", systemImage: "books.vertical")
                }
                .tag(0)

            RecentView()
                .tabItem {
                    Label("Recent", systemImage: "clock")
                }
                .tag(1)

            ArchiveView()
                .tabItem {
                    Label("Archive", systemImage: "archivebox")
                }
                .tag(2)

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gear")
                }
                .tag(3)
        }
    }
}

#if DEBUG
struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(AppState())
            .environmentObject(DocumentManager())
    }
}
#endif

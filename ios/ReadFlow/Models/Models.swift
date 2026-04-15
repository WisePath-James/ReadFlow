import Foundation
import SwiftUI

// MARK: - 文档模型
struct Document: Identifiable, Codable, Hashable {
    let id: UUID
    var title: String
    var fileType: FileType
    var folderId: UUID?
    var pageCount: Int
    var processingStatus: ProcessingStatus
    var outline: DocumentOutline?
    var createdAt: Date
    var updatedAt: Date

    enum FileType: String, Codable, CaseIterable {
        case pdf
        case doc, docx
        case epub
        case txt
        case markdown = "md"
        case html
        case rtf
        case xml
        case json

        var icon: String {
            switch self {
            case .pdf: return "doc.fill"
            case .doc, .docx: return "doc.fill"
            case .epub: return "book.fill"
            case .txt: return "doc.plaintext.fill"
            case .markdown: return "chevron.left.forwards"
            case .html: return "globe"
            case .rtf: return "doc.fill"
            case .xml: return "chevron.left.forwards"
            case .json: return "brackets"
            }
        }

        var color: Color {
            switch self {
            case .pdf: return .red
            case .doc, .docx: return .blue
            case .epub: return .orange
            case .txt, .markdown: return .gray
            case .html: return .cyan
            case .rtf: return .purple
            case .xml, .json: return .yellow
            }
        }
    }

    enum ProcessingStatus: String, Codable {
        case pending
        case processing
        case completed
        case failed
    }
}

// MARK: - 文档大纲
struct DocumentOutline: Codable, Hashable {
    var title: String
    var pageIndex: Int
    var children: [DocumentOutline]?
}

// MARK: - 文件夹模型
struct Folder: Identifiable, Codable, Hashable {
    let id: UUID
    var name: String
    var color: Color
    var icon: String
    var parentId: UUID?
    var documentCount: Int
    var createdAt: Date
    var updatedAt: Date

    // 计算属性：未读数量（待实现）
    var unreadCount: Int { 0 }

    init(id: UUID = UUID(), name: String, color: Color = .blue, icon: String = "folder", parentId: UUID? = nil, documentCount: Int = 0, createdAt: Date = Date(), updatedAt: Date = Date()) {
        self.id = id
        self.name = name
        self.color = color
        self.icon = icon
        self.parentId = parentId
        self.documentCount = documentCount
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

// MARK: - 标注模型
struct Annotation: Identifiable, Codable, Hashable {
    let id: UUID
    var documentId: UUID
    var pageIndex: Int
    var type: AnnotationType
    var color: Color
    var quote: String
    var highlightAreas: [HighlightArea]
    var createdAt: Date

    enum AnnotationType: String, Codable, CaseIterable {
        case highlight
        case underline
        case strikeout
    }

    struct HighlightArea: Codable, Hashable {
        var bounds: CGRect
        var pageBounds: CGRect

        enum CodingKeys: String, CodingKey {
            case bounds, pageBounds
        }
    }
}

// MARK: - 笔记模型
struct Note: Identifiable, Codable, Hashable {
    let id: UUID
    var documentId: UUID
    var pageIndex: Int
    var quote: String
    var highlightAreas: [Annotation.HighlightArea]
    var noteText: String
    var createdAt: Date
    var updatedAt: Date
}

// MARK: - 手写批注模型
struct HandwritingAnnotation: Identifiable, Codable, Hashable {
    let id: UUID
    var documentId: UUID
    var pageIndex: Int
    var drawingData: Data // PKDrawing 数据
    var toolType: PencilTool
    var color: Color
    var strokeWidth: Double
    var anchorMeta: AnchorMeta
    var createdAt: Date
    var updatedAt: Date

    enum PencilTool: String, Codable {
        case pen
        case pencil
        case marker
        case eraser
        case lasso
    }

    struct AnchorMeta: Codable, Hashable {
        var pageBounds: CGRect
        var virtualPageIndex: Int?
        var blockAnchor: String?
    }
}

// MARK: - 阅读进度
struct ReadingProgress: Codable, Hashable {
    var userId: UUID
    var documentId: UUID
    var currentPage: Int
    var scrollOffset: CGFloat
    var zoomLevel: CGFloat
    var readingMode: ReadingMode
    var theme: Theme
    var lastReadAt: Date
    var updatedAt: Date

    enum ReadingMode: String, Codable {
        case original  // 原页模式
        case reflow    // 文本重排模式
    }

    enum Theme: String, Codable {
        case light
        case dark
        case sepia
    }
}

// MARK: - 归档卡片
struct Archive: Identifiable, Codable, Hashable {
    let id: UUID
    var documentId: UUID
    var pageIndex: Int
    var quote: String
    var highlightAreas: [Annotation.HighlightArea]
    var question: String
    var answer: String
    var tags: [String]
    var createdAt: Date

    var document: Document? // 关联文档
}

// MARK: - AI 线程
struct AIThread: Identifiable, Codable, Hashable {
    let id: UUID
    var documentId: UUID
    var pageIndex: Int
    var selectionHash: String
    var selectionText: String
    var createdAt: Date
    var expiresAt: Date
    var mode: ThreadMode

    enum ThreadMode: String, Codable {
        case quick
        case deep
    }

    var isExpired: Bool {
        Date() > expiresAt
    }
}

// MARK: - 文档块（用于非 PDF 文档）
struct DocumentBlock: Identifiable, Codable, Hashable {
    let id: UUID
    var documentId: UUID
    var blockIndex: Int
    var sectionId: String?
    var virtualPageIndex: Int
    var blockText: String
    var anchorMeta: AnchorMeta

    struct AnchorMeta: Codable, Hashable {
        var blockId: String
        var charOffset: Int
        var charLength: Int
    }
}

// MARK: - 向量块（用于 AI 检索）
struct DocumentChunk: Identifiable, Codable, Hashable {
    let id: UUID
    var documentId: UUID
    var pageStart: Int?
    var pageEnd: Int?
    var chapterTitle: String?
    var chunkText: String
    var embedding: [Double]?
    var createdAt: Date
}

// 扩展：Color 支持 Codable
extension Color: Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let colorString = try container.decode(String.self)
        // 简化的颜色解析（实际项目需要更完整的实现）
        self = Color(hex: colorString) ?? .primary
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        // 简化的颜色编码
        try container.encode("#000000")
    }
}

// 扩展：CGRect 支持 Codable
extension CGRect: Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let x = try container.decode(CGFloat.self, forKey: .x)
        let y = try container.decode(CGFloat.self, forKey: .y)
        let width = try container.decode(CGFloat.self, forKey: .width)
        let height = try container.decode(CGFloat.self, forKey: .height)
        self.init(x: x, y: y, width: width, height: height)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(origin.x, forKey: .x)
        try container.encode(origin.y, forKey: .y)
        try container.encode(size.width, forKey: .width)
        try container.encode(size.height, forKey: .height)
    }

    private enum CodingKeys: String, CodingKey {
        case x, y, width, height
    }
}

// 辅助：从十六进制字符串创建 Color
extension Color {
    init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        var rgb: UInt64 = 0
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else {
            return nil
        }

        let r = Double((rgb & 0xFF0000) >> 16) / 255.0
        let g = Double((rgb & 0x00FF00) >> 8) / 255.0
        let b = Double(rgb & 0x0000FF) / 255.0

        self.init(red: r, green: g, blue: b)
    }
}

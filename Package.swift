// swift-tools-version: 5.9
// KovaSwift — SwiftUI bridge for Kova ViewModels.
// Framework-agnostic on purpose: it never imports your app's Kotlin framework, so it can be
// consumed as a normal Swift package. A ~40-line bridge file in your app (see
// template/iosApp/iosApp/Kova/KovaBridge.swift) adapts your framework's types to these APIs.
import PackageDescription

let package = Package(
    name: "KovaSwift",
    platforms: [
        .iOS(.v16),
        .macOS(.v13),
    ],
    products: [
        .library(name: "KovaSwift", targets: ["KovaSwift"]),
    ],
    targets: [
        .target(name: "KovaSwift", path: "kova-swift/Sources"),
    ]
)

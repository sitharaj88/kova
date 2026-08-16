import SwiftUI
import Shared
import KovaSwift

/// Pure UI: renders `TasksState`, forwards input to the shared `TasksViewModel`,
/// and reacts to one-shot actions. Mirrors the Compose screen line for line.
struct TasksScreen: View {
    @StateObject private var holder = ViewModelHolder { ViewModels.shared.tasks() }
    @State private var newTitle = ""
    @State private var message: String?

    private var viewModel: TasksViewModel { holder.viewModel }

    var body: some View {
        NavigationStack {
            Observing(viewModel.stateNative) { state in
                content(state)
                    .navigationTitle("Tasks · \(state.remaining) left")
            }
        }
        .task {
            for await action in stream(viewModel.actionsNative) {
                if let show = action as? TasksActionShowMessage {
                    message = show.text
                }
            }
        }
        .overlay(alignment: .bottom) {
            if let message {
                SnackbarView(text: message) { self.message = nil }
            }
        }
    }

    @ViewBuilder
    private func content(_ state: TasksState) -> some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                TextField("New task", text: $newTitle)
                    .textFieldStyle(.roundedBorder)
                Button("Add") {
                    viewModel.add(title: newTitle)
                    newTitle = ""
                }
                .buttonStyle(.borderedProminent)
                .disabled(state.isSaving)
            }
            .padding(.horizontal)

            if state.isLoading {
                Spacer()
                ProgressView()
                Spacer()
            } else {
                List {
                    ForEach(state.tasks, id: \.id) { task in
                        TaskRow(
                            task: task,
                            onToggle: { viewModel.toggle(id: task.id) },
                            onDelete: { viewModel.delete(id: task.id) }
                        )
                    }
                }
                .listStyle(.plain)
            }
        }
    }
}

private struct TaskRow: View {
    let task: TaskItem
    let onToggle: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack {
            Button(action: onToggle) {
                Image(systemName: task.done ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(task.done ? .green : .secondary)
            }
            .buttonStyle(.plain)

            Text(task.title)
                .strikethrough(task.done)

            Spacer()

            Button(action: onDelete) {
                Image(systemName: "trash")
                    .foregroundStyle(.red)
            }
            .buttonStyle(.plain)
        }
    }
}

private struct SnackbarView: View {
    let text: String
    let onDismiss: () -> Void

    var body: some View {
        Text(text)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(.thinMaterial, in: Capsule())
            .padding(.bottom, 16)
            .transition(.move(edge: .bottom).combined(with: .opacity))
            .task {
                try? await Task.sleep(nanoseconds: 2_500_000_000)
                onDismiss()
            }
    }
}

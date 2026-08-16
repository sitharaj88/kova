package sitharaj.kova.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore

/**
 * Owns ViewModels on platforms without a system-provided owner (iOS).
 *
 * Hold one [ViewModelHost] per SwiftUI screen (inside an `ObservableObject` created as a
 * `@StateObject`) and call [clear] from `deinit`. Every attached ViewModel then gets its
 * `onCleared()` callback and `viewModelScope` cancellation — the exact lifecycle contract
 * Android provides for free.
 *
 * ```swift
 * final class Host<VM: ViewModel>: ObservableObject {
 *     let viewModel: VM
 *     private let host = ViewModelHost()
 *     init(_ factory: () -> VM) {
 *         viewModel = factory()
 *         host.attach(viewModel: viewModel)
 *     }
 *     deinit { host.clear() }
 * }
 * ```
 */
public class ViewModelHost {

    private val store = ViewModelStore()
    private var counter = 0

    /** Registers [viewModel] so it is cleared when [clear] is called. */
    public fun attach(viewModel: ViewModel) {
        store.put("kova-vm-${counter++}", viewModel)
    }

    /** Clears every attached ViewModel, cancelling their scopes. */
    public fun clear() {
        store.clear()
    }
}

package sitharaj.kova.annotations

/**
 * Marks a ViewModel for Swift-friendly code generation.
 *
 * For every public `StateFlow<T>`, `Flow<T>` or `EventFlow<T>` property on the annotated
 * class, the Kova KSP processor generates a matching `<name>Native` extension property in
 * the iOS source set:
 *
 * ```kotlin
 * @NativeExport
 * class HomeViewModel : StateViewModel<HomeState, HomeAction>(HomeState())
 * ```
 *
 * generates
 *
 * ```kotlin
 * val HomeViewModel.stateNative: NativeStateFlow<HomeState>
 * val HomeViewModel.actionsNative: NativeFlow<HomeAction>
 * ```
 *
 * so Swift can observe state with a synchronous initial value and main-thread callbacks,
 * with the subscription lifecycle tied to `viewModelScope`.
 *
 * The annotated class must extend `androidx.lifecycle.ViewModel` (which includes every
 * `StateViewModel`).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class NativeExport

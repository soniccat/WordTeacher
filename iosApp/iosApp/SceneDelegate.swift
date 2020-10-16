import UIKit
import SwiftUI
import shared

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    private let connectivityManager = ConnectivityManager()
    var window: UIWindow?

    var configService: ConfigService!
    var configRepository: ConfigRepository!
    var configConnectParamsStatRepository: ConfigConnectParamsStatRepository!
    var serviceRepository: ServiceRepository!
    var wordTeacherWordServiceFactory: WordTeacherWordServiceFactory!
    var wordRepository: WordRepository!
    var viewModel: DefinitionsVM!

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        // Use this method to optionally configure and attach the UIWindow `window` to the provided UIWindowScene `scene`.
        // If using a storyboard, the `window` property will automatically be initialized and attached to the scene.
        // This delegate does not imply the connecting scene or session are new (see `application:configurationForConnectingSceneSession` instead).
        
        if viewModel == nil {
            configService = ConfigService(baseUrl: "https://soniccat.ru/")
            configRepository = ConfigRepository(
                service: configService,
                connectivityManager: connectivityManager)
            configConnectParamsStatRepository = ConfigConnectParamsStatRepository(
                file: ConfigConnectParamsStatFile())
            wordTeacherWordServiceFactory = WordTeacherWordServiceFactory()
            serviceRepository = ServiceRepository(
                configRepository: configRepository,
                connectParamsStatRepository: configConnectParamsStatRepository,
                serviceFactory: wordTeacherWordServiceFactory)
            wordRepository = WordRepository(serviceRepository: serviceRepository)
            
            viewModel = DefinitionsVM(
                connectivityManager: connectivityManager,
                wordRepository: wordRepository,
                state: DefinitionsVM.State.init(word: nil))
        }

        // Create the SwiftUI view that provides the window contents.
        let contentView = ContentView()

        // Use a UIHostingController as window root view controller.
        if let windowScene = scene as? UIWindowScene {
            let window = UIWindow(windowScene: windowScene)
            window.rootViewController = UIHostingController(rootView: contentView)
            self.window = window
            window.makeKeyAndVisible()
        }
        
        viewModel.definitions.addObserver { (res: Resource<NSArray>?) in
            let t = type(of: res)
            let isLoaded = res!.isLoaded() ? "true" : "false"
            print("status '\(t)' isLoaded: \(isLoaded)")
        }
    }

    func sceneDidDisconnect(_ scene: UIScene) {
        // Called as the scene is being released by the system.
        // This occurs shortly after the scene enters the background, or when its session is discarded.
        // Release any resources associated with this scene that can be re-created the next time the scene connects.
        // The scene may re-connect later, as its session was not neccessarily discarded (see `application:didDiscardSceneSessions` instead).
    }

    func sceneDidBecomeActive(_ scene: UIScene) {
        // Called when the scene has moved from an inactive state to an active state.
        // Use this method to restart any tasks that were paused (or not yet started) when the scene was inactive.
        connectivityManager.register()
        connectivityManager.checkNetworkState()
    }

    func sceneWillResignActive(_ scene: UIScene) {
        // Called when the scene will move from an active state to an inactive state.
        // This may occur due to temporary interruptions (ex. an incoming phone call).
        connectivityManager.unregister()
    }

    func sceneWillEnterForeground(_ scene: UIScene) {
        // Called as the scene transitions from the background to the foreground.
        // Use this method to undo the changes made on entering the background.
    }

    func sceneDidEnterBackground(_ scene: UIScene) {
        // Called as the scene transitions from the foreground to the background.
        // Use this method to save data, release shared resources, and store enough scene-specific state information
        // to restore the scene back to its current state.
    }
}


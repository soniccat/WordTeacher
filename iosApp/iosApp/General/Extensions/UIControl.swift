//
//  UIControl.swift
//  iosApp
//
//  Created by Alexey Glushkov on 15.11.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

@objc class ClosureSleeve: NSObject {
    weak var control: UIControl?
    let closure: (UIControl?)->()

    init (control: UIControl, _ closure: @escaping (UIControl?)->()) {
        self.control = control
        self.closure = closure
    }

    @objc func invoke () {
        closure(self.control)
    }
}

extension UIControl {
    func setAction(
        event: UIControl.Event,
        _ closure: @escaping (UIControl?)->()
    ) {
        let sleeve = ClosureSleeve(control: self, closure)
        addTarget(sleeve, action: #selector(ClosureSleeve.invoke), for: event)
        objc_setAssociatedObject(self, event.toActionPointer(), sleeve, objc_AssociationPolicy.OBJC_ASSOCIATION_RETAIN)
    }
    
    func removeAction(
        event: UIControl.Event
    ) {
        let actionPointer = event.toActionPointer()
        if let sleeve = objc_getAssociatedObject(self, actionPointer) as? ClosureSleeve {
            removeTarget(sleeve, action: nil, for: event)
            objc_setAssociatedObject(self, actionPointer, nil, objc_AssociationPolicy.OBJC_ASSOCIATION_RETAIN)
        }
    }
}

extension UIControl.Event {
    private struct AssociatedKeys {
        static let ActionEventValueChangedAction: NSString = "valueChangedAction"
        static let ActionEventTouchDownAction: NSString = "touchDownAction"
        static let ActionEventTouchDownRepeatAction: NSString = "touchDownRepeatAction"
        static let ActionEventTouchDragInsideAction: NSString = "touchDragInsideAction"
        static let ActionEventTouchDragOutsideAction: NSString = "touchDragOutsideAction"
        static let ActionEventTouchDragEnterAction: NSString = "touchDragEnterAction"
        static let ActionEventTouchDragExitAction: NSString = "touchDragExitAction"
        static let ActionEventTouchUpInsideAction: NSString = "touchUpInsideAction"
        static let ActionEventTouchUpOutsideAction: NSString = "touchUpOutsideAction"
        static let ActionEventTouchCancelAction: NSString = "touchCancelAction"
        static let ActionEventUnknownAction: NSString = "unknownAction"
    }
    
    func toActionPointer() -> UnsafeRawPointer {
        switch self {
        case .valueChanged: return getRawPointer(AssociatedKeys.ActionEventValueChangedAction)
        case .touchDown: return getRawPointer(AssociatedKeys.ActionEventTouchDownAction)
        case .touchDownRepeat: return getRawPointer(AssociatedKeys.ActionEventTouchDownRepeatAction)
        case .touchDragInside: return getRawPointer(AssociatedKeys.ActionEventTouchDragInsideAction)
        case .touchDragOutside: return getRawPointer(AssociatedKeys.ActionEventTouchDragOutsideAction)
        case .touchDragEnter: return getRawPointer(AssociatedKeys.ActionEventTouchDragEnterAction)
        case .touchDragExit: return getRawPointer(AssociatedKeys.ActionEventTouchDragExitAction)
        case .touchUpInside: return getRawPointer(AssociatedKeys.ActionEventTouchUpInsideAction)
        case .touchUpOutside: return getRawPointer(AssociatedKeys.ActionEventTouchUpOutsideAction)
        case .touchCancel: return getRawPointer(AssociatedKeys.ActionEventTouchCancelAction)
        default: return getRawPointer(AssociatedKeys.ActionEventUnknownAction)
        }
    }

    private func getRawPointer(_ obj: AnyObject) -> UnsafeRawPointer {
        return UnsafeRawPointer(Unmanaged.passUnretained(obj).toOpaque())
    }
}

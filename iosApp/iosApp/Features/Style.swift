//
//  Design.swift
//  iosApp
//
//  Created by Alexey Glushkov on 23.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

class Style {
    static let wordTitleTextAppearance = TextAppearances.headerTextAppearance
    static let wordProvidedByTextAppearance = TextAppearances.bodyTextAppearance
    static let wordTranscriptionTextAppearance = TextAppearances.bodyTextAppearance
    static let wordPartOfSpeechTextAppearance = TextAppearances.header2TextAppearance
    static let wordDefinitionTextAppearance = TextAppearances.bodyTextAppearance
    static let wordExampleTextAppearance = TextAppearances.bodyTextAppearance
    static let wordSynonymTextAppearance = TextAppearances.bodyTextAppearance
    static let wordSubHeaderTextAppearance = TextAppearances.subtitleTextAppearance
    
    static let dividerColor = Colors.lightGray
    static let dividerHeight: CGFloat = 1
    static let dividerLeftMargin: CGFloat = 36
}

private class TextAppearances {
    static let headerTextAppearance = TextAppearance(
        font: UIFont.preferredFont(forTextStyle: .headline),
        textColor: UIColor.black
    )
    
    static let header2TextAppearance = TextAppearance(
        font: UIFont.preferredFont(forTextStyle: .subheadline),
        textColor: UIColor.black
    )
    
    static let subtitleTextAppearance = TextAppearance(
        font: UIFont.preferredFont(forTextStyle: .caption1),
        textColor: UIColor.black
    )
    
    static let bodyTextAppearance = TextAppearance(
        font: UIFont.preferredFont(forTextStyle: .body),
        textColor: UIColor.black
    )
}

struct TextAppearance{
    let font: UIFont
    let textColor: UIColor
    
    func toAttributes() -> [NSAttributedString.Key : Any] {
        return [
            .font : font,
            .foregroundColor : textColor
        ]
    }
}

private struct Colors {
    static let lightGray = UIColor.lightGray
}

extension UILabel {
    func applyTextAppearance(_ appearance: TextAppearance) {
        font = appearance.font
        textColor = appearance.textColor
    }
}

extension UISegmentedControl {
    func applyTextAppearance(_ appearance: TextAppearance) {
        setTitleTextAttributes(appearance.toAttributes(), for: .normal)
    }
}

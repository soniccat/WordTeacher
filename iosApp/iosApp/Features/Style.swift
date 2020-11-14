//
//  Design.swift
//  iosApp
//
//  Created by Alexey Glushkov on 23.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit

class Style {
    static let wordTitleTextAppearance = TextAppearances.titleTextAppearance
    static let wordProvidedByTextAppearance = TextAppearances.bodyTextAppearance
    static let wordTranscriptionTextAppearance = TextAppearances.bodyTextAppearance
    static let wordPartOfSpeechTextAppearance = TextAppearances.title2SemiboldTextAppearance
    static let wordDefinitionTextAppearance = TextAppearances.bodyTextAppearance
    static let wordExampleTextAppearance = TextAppearances.bodyTextAppearance
    static let wordSynonymTextAppearance = TextAppearances.bodyTextAppearance
    static let wordSubHeaderTextAppearance = TextAppearances.headlineTextAppearance
    
    static let dividerColor = Colors.lightGray
    static let dividerHeight: CGFloat = 1
    static let dividerLeftMargin: CGFloat = 0
    
    static let cellTopMargin: CGFloat = 0
    static let cellBottomMargin: CGFloat = 0
    
    static let cellDividerTopMargin: CGFloat = 10
    static let cellDividerBottomMargin: CGFloat = 12
    static let cellPartOfSpeechTopMargin: CGFloat = 8
    static let cellSubHeaderTopMargin: CGFloat = 6
    static let cellHeaderBottomMargin: CGFloat = 2
    static let cellDefinitionsDisplayVerticalMargin: CGFloat = 12
}

private class TextAppearances {
    static let titleTextAppearance = TextAppearance(
        font: UIFont.preferredFont(forTextStyle: .title1),
        textColor: UIColor.black
    )
    
    static let title2SemiboldTextAppearance = TextAppearance(
        font: UIFontDescriptor.preferredFontDescriptor(withTextStyle: .title2).toSemibold().toFont(),
        textColor: UIColor.black
    )
    
    static let headlineTextAppearance = TextAppearance(
        font: UIFont.preferredFont(forTextStyle: .headline),
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

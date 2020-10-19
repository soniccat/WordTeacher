//
//  SimpleAdapter.swift
//  iosApp
//
//  Created by Alexey Glushkov on 18.10.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import UIKit
import shared

enum Section {
  case main
}

class SimpleAdapter {
    var dataSource: UICollectionViewDiffableDataSource<Section, BaseViewItem<AnyObject>>
    
    init(collectionView: UICollectionView) {
        collectionView.register(UINib(nibName: "TextCell", bundle: Bundle.main), forCellWithReuseIdentifier: "TextCell")
        
        dataSource = UICollectionViewDiffableDataSource<Section, BaseViewItem<AnyObject>>(
        collectionView: collectionView) { (collectionView, indexPath, item) -> UICollectionViewCell? in
            let cell = collectionView.dequeueReusableCell(
              withReuseIdentifier: "TextCell",
              for: indexPath) as? TextCell
            cell?.textView.text = item.firstItem() as? String ?? "class " + type(of: item).description()
            return cell
        }
        
    }
}

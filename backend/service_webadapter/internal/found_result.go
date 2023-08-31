package internal

import "golang.org/x/net/html"

type FoundResult struct {
	ParentNode *html.Node

	//ChildIndex int
	ChildNode *html.Node

	//ChildIndexInOriginalNode int
	ChildNodeInOriginalNode *html.Node
}

func NewFoundResultWithNode(n *html.Node) *FoundResult {
	return NewFoundResult(n, nil, nil)
}

func NewFoundResult(n *html.Node, childNode *html.Node, childNodeInOriginalNode *html.Node) *FoundResult {
	return &FoundResult{
		ParentNode:              n,
		ChildNode:               childNode,
		ChildNodeInOriginalNode: childNodeInOriginalNode,
	}
}

// func NewFoundResult(n *html.Node, chIndex int, chIndexInOriginalNode int) *FoundResult {
// 	return &FoundResult{
// 		Node:                     n,
// 		ChildIndex:               chIndex,
// 		ChildIndexInOriginalNode: chIndexInOriginalNode,
// 	}
// }

// func (f *FoundResult) textContent() *string {
// 	// if (this.node instanceof Element) {
// 	// 	return this.node.textContent
// 	// } else {
// 	// 	return null
// 	// }
// 	return nil
// }

// func (f *FoundResult) childNodes() []html.Node {

// 	let n = this.node
// 	if (n instanceof Node) {
// 		return n.childNodes
// 	} else {
// 		return null
// 	}
// }

// func (f *FoundResult) childNode() html.Node {
// 	p := f.node.Parent

// 	return f.node.childNodes[this.childIndex]
// }

// func GetNodeAtIndex(node *html.Node, index int) *html.Node {
// 	p := node.Parent
// }

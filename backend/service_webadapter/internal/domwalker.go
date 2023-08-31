package internal

import (
	"fmt"
	"strings"

	"golang.org/x/net/html"

	"tools"
)

type DOMWalker struct {
	cursorStack       []DOMWalkerCursor
	cursor            *DOMWalkerCursor
	lastFoundResult   *FoundResult
	internalDOMWalker *DOMWalker
	stateStack        []DOMWalkerState
}

func (d *DOMWalker) pushState() *DOMWalker {
	d.stateStack = append(d.stateStack, *NewDOMWalkerState(d.cursorStack, d.cursor, d.lastFoundResult))

	return d
}

func (d *DOMWalker) popState() *DOMWalker {
	len := len(d.stateStack)
	if len == 0 {
		return d
	}

	state := d.stateStack[len-1]
	d.stateStack = d.stateStack[:len-1]
	d.cursorStack = state.cursorStack
	d.cursor = state.cursor
	d.lastFoundResult = state.lastFoundResult

	return d
}

func (d *DOMWalker) goIn() *DOMWalker {
	c := d.cursor
	d.cursorStack = append(d.cursorStack, *d.cursor)
	d.cursor = c.inCursor()

	return d
}

func (d *DOMWalker) goToFoundResult() (*DOMWalker, error) {
	if d.lastFoundResult == nil {
		return nil, NewDOMWalkerError("goIn() failed as lastFoundResult is null")
	}

	d.cursorStack = append(d.cursorStack, *d.cursor)
	d.cursor = NewDOMWalkerCursor(d.lastFoundResult.ParentNode, d.lastFoundResult.ChildNode, nil)

	return d, nil
}

func (d *DOMWalker) goInFoundResult() (*DOMWalker, error) {
	if d.lastFoundResult == nil {
		return nil, NewDOMWalkerError(`goIn() failed as lastFoundResult is null`)
	}

	d.cursorStack = append(d.cursorStack, *d.cursor)
	d.cursor = NewDOMWalkerCursorWithParentNode(d.lastFoundResult.ChildNode)

	return d, nil
}

func (d *DOMWalker) goOut() *DOMWalker {
	len := len(d.cursorStack)
	if len == 0 {
		return d
	}

	d.cursor = tools.Ptr(d.cursorStack[len-1])
	d.cursorStack = d.cursorStack[0 : len-1]

	return d
}

func (d *DOMWalker) findNodeContainingText(text string) (*DOMWalker, error) {
	fr := findNodeContainingText(d.cursor.parentNode, d.cursor.domRange(), text)
	if fr != nil {
		d.lastFoundResult = fr
		d.cursor.childNode = fr.ChildNodeInOriginalNode
	} else {
		return nil, NewDOMWalkerError(fmt.Sprintf("findNodeContainingText(%s) failed at %s}", text, d.cursor.toString()))
	}

	return d, nil
}

// internals

type DOMWalkerError struct {
	message string
}

func NewDOMWalkerError(message string) *DOMWalkerError {
	return &DOMWalkerError{message}
}

func (e *DOMWalkerError) Error() string {
	return e.message
}

type DOMWalkerCursor struct {
	parentNode *html.Node
	childNode  *html.Node
	lastNode   *html.Node
}

func NewDOMWalkerCursorWithParentNode(parentNode *html.Node) *DOMWalkerCursor {
	return &DOMWalkerCursor{
		parentNode,
		parentNode.FirstChild,
		parentNode.LastChild,
	}
}

func NewDOMWalkerCursor(parentNode *html.Node, childNode *html.Node, lastNode *html.Node) *DOMWalkerCursor {
	if childNode == nil {
		childNode = parentNode.FirstChild
	}

	if lastNode == nil {
		lastNode = parentNode.LastChild
	}

	return &DOMWalkerCursor{
		parentNode,
		childNode,
		lastNode,
	}
}

func (c *DOMWalkerCursor) getChildNodeIndex() int {
	if c.childNode == nil {
		return -1
	}

	if c.parentNode != c.childNode.Parent {
		panic("c.parentNode != c.childNode.Parent")
	}

	return GetNodeIndex(c.childNode)
}

func (c *DOMWalkerCursor) getLastNodeIndex() int {
	if c.lastNode == nil {
		return -1
	}

	if c.parentNode != c.lastNode.Parent {
		panic("c.parentNode != c.lastNode.Parent")
	}

	return GetNodeIndex(c.lastNode)
}

func (c *DOMWalkerCursor) inCursor() *DOMWalkerCursor {
	return NewDOMWalkerCursorWithParentNode(c.childNode)
}

func (c *DOMWalkerCursor) toString() string {
	return fmt.Sprintf("node: %s\nchildIndex: %d\nmaxIndex: %d", GetTextContent(c.parentNode), c.getChildNodeIndex(), c.getLastNodeIndex())
}

func (c *DOMWalkerCursor) domRange() DOMWalkerRange {
	return NewDOMWalkerRange(c.childNode, c.lastNode)
}

func (c *DOMWalkerCursor) atEnd() bool {
	return c.lastNode != nil && c.childNode == c.lastNode || c.childNode == c.parentNode.LastChild
}

// [startNode..endNode)
type DOMWalkerRange struct {
	startNode *html.Node
	endNode   *html.Node
}

func NewDOMWalkerRange(startNode *html.Node, endNode *html.Node) DOMWalkerRange {
	return DOMWalkerRange{startNode, endNode}
}

// func (r *DOMWalkerRange) IsInRange(v int) bool {
// 	return v >= r.start && (r.end == -1 || v < r.end)
// }

func (r *DOMWalkerRange) RangeWithStart(startNode *html.Node) DOMWalkerRange {
	return NewDOMWalkerRange(startNode, r.endNode)
}

func (r *DOMWalkerRange) RangeWithEnd(endNode *html.Node) DOMWalkerRange {
	return NewDOMWalkerRange(r.startNode, endNode)
}

var NodeRange = NewDOMWalkerRange(nil, nil)

type DOMWalkerState struct {
	cursorStack     []DOMWalkerCursor
	cursor          *DOMWalkerCursor
	lastFoundResult *FoundResult
}

func NewDOMWalkerState(cursorStack []DOMWalkerCursor, cursor *DOMWalkerCursor, lastFoundResult *FoundResult) *DOMWalkerState {
	stack := make([]DOMWalkerCursor, 0, len(cursorStack))
	stack = append(stack, cursorStack...)

	return &DOMWalkerState{
		stack,
		NewDOMWalkerCursor(cursor.parentNode, cursor.childNode, cursor.lastNode),
		lastFoundResult,
	}
}

// search functions

func findNodeContainingText(startNode *html.Node, r DOMWalkerRange, text string) *FoundResult {
	return findNodeContainingWithTextChecker(startNode, r, func(nodeText string) bool {
		return strings.Contains(nodeText, text)
	})
}

func findNodeContainingWithTextChecker(startNode *html.Node, r DOMWalkerRange, textChecker func(t string) bool) *FoundResult {
	var fr *FoundResult
	OnEachChildNode(startNode, r, func(node *html.Node) bool {
		fr = findNodeContainingWithTextChecker(node, NodeRange, textChecker)
		if fr != nil {
			if fr.ChildNode == nil {
				fr = NewFoundResult(startNode, node, node)
				return true
			}

			fr.ChildNodeInOriginalNode = node
			return true
		}

		return false
	})

	if fr != nil {
		return fr
	}

	if r.startNode == nil {
		if textChecker(GetTextContent(startNode)) {
			return NewFoundResultWithNode(startNode)
		}
	}

	return nil
}

// tool functions

func GetTextContent(node *html.Node) string {
	b := strings.Builder{}
	AddTextContentWithBuilder(node, &b)
	return b.String()
}

func AddTextContentWithBuilder(node *html.Node, builder *strings.Builder) {
	builder.WriteString(node.Data)

	OnEachChildNode(node, NodeRange, func(node *html.Node) bool {
		AddTextContentWithBuilder(node, builder)
		return false
	})
}

func OnEachChildNode(parent *html.Node, r DOMWalkerRange, f func(node *html.Node) bool) {
	for n := r.startNode; n != r.endNode; {
		f(n)
		n = n.NextSibling
	}
}

func GetNodeAtIndex(parent *html.Node, index int) *html.Node {
	r := parent.FirstChild
	for i := 1; i <= index; i++ {
		r = r.NextSibling
		if r == nil {
			return nil
		}
	}

	return r
}

func GetNodeIndex(node *html.Node) int {
	if node.Parent == nil {
		return -1
	}

	i := 0
	n := node.Parent.FirstChild
	for ; n != node; i++ {
		n = n.NextSibling
	}

	return i
}

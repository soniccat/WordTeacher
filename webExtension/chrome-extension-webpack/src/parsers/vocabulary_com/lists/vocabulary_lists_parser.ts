import * as dwlib from "../../../dom/domwalker"
import { CardSetBuilder } from '../../../builder/cardset_builder';
import { ParserResult } from "../../parser_result";

export function vocabularyListParser(document: Document): ParserResult {
  const cardSetBuilder = new CardSetBuilder("", document.URL)
  let titleNode: Node

  const dw = new dwlib.DOMWalker(new dwlib.DOMWalkerCursor(document.body))
  dw
  .pushState()
  .goToTopNodeWithMatcher(new dwlib.ClassNodeMatcher("title-actions-stats"))
  .goToTopNodeWithMatcher(new dwlib.ClassNodeMatcher("title"))
  .childNode((node) => {
    titleNode = node
  })
  .textContent((t) => {
    console.log("title: " + t.trim()) 
    cardSetBuilder.setName(t.trim().replace("\n", " ").replace("  ", " "))
  })
  .popState()
  .goToTopNodeWithMatcher(new dwlib.ClassNodeMatcher("entry", dwlib.ClassNodeMatcherType.StartsWith))
  .splitByMatcherWithDOMWalker(
    new dwlib.ClassNodeMatcher("entry", dwlib.ClassNodeMatcherType.StartsWith),
    (dw) => {
      dw
      .goIn()
      .findTopNodeWithMatcher(new dwlib.ClassNodeMatcher("word"))
      .textContent((t) => { 
        console.log(t.trim()) 
        cardSetBuilder.startCard()
        cardSetBuilder.setCardTerm(t.trim().replace("\n", " ").replace("  ", " "))
      })
      .findTopNodeWithMatcher(new dwlib.ClassNodeMatcher("definition"))
      .textContent((t) => { 
        console.log(t.trim()) 
        cardSetBuilder.addCardDefinition(t.trim().replace("\n", " ").replace("  ", " "))
      })
      .try((dw) => {
        dw
        .findTopNodeWithMatcher(new dwlib.ClassNodeMatcher("example"))
        .textContent((t) => { 
          console.log(t.trim()) 
          cardSetBuilder.addCardExample(t.trim().replace("\n", " ").replace("  ", " "))
        })
      })
    }
  )

  return {
    cardSetBuilder: cardSetBuilder,
    titleNode: titleNode
  }
}

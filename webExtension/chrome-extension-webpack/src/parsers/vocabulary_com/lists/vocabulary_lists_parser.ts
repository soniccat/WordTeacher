import * as dwlib from "../../../domwalker"
import { CardSetBuilder } from '../../../cardset_builder';
import { ParserResult } from "../../../parser_result";

export function vocabularyListParser(document: Document): ParserResult {
  const cardSetBuilder = new CardSetBuilder("", document.URL)
  let titleNode: Node

  const dw = new dwlib.DOMWalker(new dwlib.DOMWalkerCursor(document.body))
  dw
  .pushState()
  .goToNodeWithClass("title-actions-stats")
  .goToNodeWithClass("title")
  .childNode((node) => {
    titleNode = node
  })
  .textContent((t) => {
    console.log("title: " + t.trim()) 
    cardSetBuilder.setName(t.trim().replace("\n", " ").replace("  ", " "))
  })
  .popState()
  .goToNodeWithClass("entry learnable")
  .splitByFunctionWithDOMWalker(
    dwlib.findNodeWithClassSplitter("entry learnable"),
    (dw) => {
      dw
      .goIn()
      .findNodeWithClass("word")
      .textContent((t) => { 
        console.log(t.trim()) 
        cardSetBuilder.startCard()
        cardSetBuilder.setCardTerm(t.trim().replace("\n", " ").replace("  ", " "))
      })
      .findNodeWithClass("definition")
      .textContent((t) => { 
        console.log(t.trim()) 
        cardSetBuilder.addCardDefinition(t.trim().replace("\n", " ").replace("  ", " "))
      })
      // .try((dw) => {
      //   dw
      //   .findNodeWithClass("example")
      //   .textContent((t) => { 
      //     console.log(t.trim()) 
      //     cardSetBuilder.addCardExample(t.trim().replace("\n", " ").replace("  ", " "))
      //   })
      // })
    }
  )

  return {
    cardSetBuilder: cardSetBuilder,
    titleNode: titleNode
  }
}

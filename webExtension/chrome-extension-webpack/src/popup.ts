import '../styles/popup.scss';
import { getStorageData } from './storage';

document.getElementById('go-to-options').addEventListener('click', () => {
  //chrome.runtime.openOptionsPage();

  getStorageData().then((obj) => {
    const element = document.getElementById('json-text')
    //document.getElementById('json-text').textContent = memStorage.cardSetJson

    // const jsonString = JSON.stringify(memStorage.cardSetJson, null, 4)
    // navigator.clipboard.writeText(jsonString).then(() => {
    //   element.textContent = "success"
    // }, () => {
    //   element.textContent = "failed"
    // });
  })
});

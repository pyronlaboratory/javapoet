/**
 * @description Updates the display state of HTML elements based on a given type
 * parameter, by setting the `display` property to '' and applying an alternative
 * styles class depending on the element's methods property modulo 2.
 * 
 * @param { integer } type - 16-bit value that determines which method will be
 * displayed, with values ranging from 0 to 65535.
 */
function show(type)
{
    count = 0;
    for (var key in methods) {
        var row = document.getElementById(key);
        if ((methods[key] &  type) != 0) {
            row.style.display = '';
            row.className = (count++ % 2) ? rowColor : altColor;
        }
        else
            row.style.display = 'none';
    }
    updateTabs(type);
}

/**
 * @description Updates the CSS class and content of HTML elements with a specific
 * ID based on the value of a parameter `type`.
 * 
 * @param { string } type - specific tab to be updated.
 */
function updateTabs(type)
{
    for (var value in tabs) {
        var sNode = document.getElementById(tabs[value][0]);
        var spanNode = sNode.firstChild;
        if (value == type) {
            sNode.className = activeTableTab;
            spanNode.innerHTML = tabs[value][1];
        }
        else {
            sNode.className = tableTab;
            spanNode.innerHTML = "<a href=\"javascript:show("+ value + ");\">" + tabs[value][1] + "</a>";
        }
    }
}

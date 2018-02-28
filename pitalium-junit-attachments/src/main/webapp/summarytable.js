/**テスト結果の集計テーブルを作成，テーブルクリックによる結果抽出を行うスクリプト*/
function GenSummaryTable(){
    element = document.getElementById("ptlcondition");
    if(element==null)return;
    if(element.getAttribute("conditionshowtable")=="false")return;
    pkg=element.getAttribute("conditionpkg");
    cls=element.getAttribute("conditioncls");


    //横軸：OS(platform)×ブラウザ(browserName)
    //縦軸：エラー名(errName)×エラー箇所（errLocation）
    //変更可能
    var keyC1="platform";
    var keyC2="browserName";
    var keyR1="errName";
    var keyR2="errLocation";

    //{C1:{C2:{R1:{C2:[count,[エラー名リスト]]}}}}
    //当該エラーのカウント
    dataTable={};

    //colum1,colum2
    //列と行のタイトル
    var columns={};
    var rows={};

    if(pkg!=null){
        resultdata={[pkg]:resultdata[pkg]};
        if(cls!=null){
            resultdata={[pkg]:{[cls]:resultdata[pkg][cls]}};
        }
    }

    for(pkgResult in resultdata){
        for(clsResult in resultdata[pkgResult]){
            for(caseResult in resultdata[pkgResult][clsResult]){
                var data=resultdata[pkgResult][clsResult][caseResult];
                if(data[keyR1]=="SUCCESS")continue;//SUCCESSを結果から省く場合（本体側でSUCCESSを出力させない方法もある）

                if(!(data[keyC1] in dataTable)){
                    dataTable[data[keyC1]]={};
                    columns[data[keyC1]]={};
                }
                if(!(data[keyC2] in dataTable[data[keyC1]])){
                    dataTable[data[keyC1]][data[keyC2]]={};
                    columns[data[keyC1]][data[keyC2]]=0;
                }
                if(!(data[keyR1] in dataTable[data[keyC1]][data[keyC2]])){
                    dataTable[data[keyC1]][data[keyC2]][data[keyR1]]={};
                    if(!(data[keyR1] in rows)){
                        rows[data[keyR1]]={};
                    }
                }
                if(!(data[keyR2] in dataTable[data[keyC1]][data[keyC2]][data[keyR1]])){
                    dataTable[data[keyC1]][data[keyC2]][data[keyR1]][data[keyR2]]=[0,[]];
                    if(!(data[keyR2] in rows[data[keyR1]])){
                        rows[data[keyR1]][data[keyR2]]=0;
                    }
                }
                dataTable[data[keyC1]][data[keyC2]][data[keyR1]][data[keyR2]][0]++;
                var testName=clsResult+"."+caseResult;
                dataTable[data[keyC1]][data[keyC2]][data[keyR1]][data[keyR2]][1].push(testName);
            }
        }
    }

    //セルのｘ座標に対応したタイトル
    titleC1=[];
    titleC2=[];
    //セルのｙ座標に対応したタイトル
    titleR1=[];
    titleR2=[];

    //タグを吐く
    table = document.createElement("table");
    table.id="PitaResultTable";
    tr = document.createElement("tr");
    table.appendChild(tr);
    //左上の余白セル
    td = document.createElement("td");
    td.setAttribute("colspan",2);
    td.setAttribute("rowspan",2);
    td.setAttribute("onClick", "PitaResultExtract(-3,-3)");
    td.id="pitacell_-3_-3";
    tr.appendChild(td);

    //1行目
    tr2 = document.createElement("tr");
    table.appendChild(tr2);
    var count_column = 0;
    for(var c1 in columns){
        //セル
        var length=Object.keys(columns[c1]).length;
        td = document.createElement("td");
        td.innerHTML=c1;
        td.setAttribute("colspan",length);
        td.setAttribute("onClick", "PitaResultExtract("+count_column+",-2)");
        td.id="pitacell_"+count_column+"_-2";
        td.className="PitaLabel";
        titleC1=titleC1.concat(new Array(length).fill(c1));
        tr.appendChild(td);
        //2行目
        for(var c2 in columns[c1]){
            //セル
            td = document.createElement("td");
            td.innerHTML=c2;
            td.setAttribute("onClick", "PitaResultExtract("+count_column+",-1)");
            td.id="pitacell_"+count_column+"_-1";
            td.className="PitaLabel";
            titleC2.push(c2);
            tr2.appendChild(td);
            count_column++;
        }

    }
    td = document.createElement("td");
    td.innerHTML="TOTAL";
    td.setAttribute("rowspan",2);
    td.className="PitaLabel";
    tr.appendChild(td);

    var count_row=0;//何行目？
    var subtotal_column=new Array(titleC1.length).fill(0);
    var total=0;
    for(var r1 in rows){
        //3行目
        tr3 = document.createElement("tr");
        table.appendChild(tr3);
        //セル
        td = document.createElement("td");
        td.innerHTML=r1;
        td.setAttribute("rowspan",Object.keys(rows[r1]).length);
        td.setAttribute("onClick", "PitaResultExtract(-2,"+count_row+")");
        td.id="pitacell_-2_"+count_row;
        td.className="PitaLabel";
        titleR1=titleR1.concat(new Array(Object.keys(rows[r1]).length).fill(r1));
        tr3.appendChild(td);
        var notfirst=false;
        for(var r2 in rows[r1]){
            if(notfirst) {
                //4行目以降～
                tr3 = document.createElement("tr");
                table.appendChild(tr3);
            }
            //セル
            td = document.createElement("td");
            td.innerHTML=r2;
            td.setAttribute("onClick", "PitaResultExtract(-1,"+count_row+")");
            td.id="pitacell_-1_"+count_row;
            td.className="PitaLabel";
            titleR2.push(r2);
            tr3.appendChild(td);

            //セル(値部分）
            var subtotal=0;
            for(var count_column = 0; count_column < titleC1.length; count_column++) {
                td = document.createElement("td");
                try{
                    val=dataTable[titleC1[count_column]][titleC2[count_column]][titleR1[count_row]][titleR2[count_row]][0];
                    if(typeof val === "undefined")throw val;//値がないときにエラー出力
                    subtotal+=val;
                    total+=val;
                    subtotal_column[count_column]+=val;
                    td.innerHTML=val;
                    td.setAttribute("onClick", "PitaResultExtract("+count_column+","+count_row+")");
                    td.id="pitacell_"+count_column+"_"+count_row;
                }catch(e){
                    td.innerHTML=0;
                }
                tr3.appendChild(td);
            }

            //セル(SUBTOTAL)
            td = document.createElement("td");
            td.innerHTML=subtotal.toString();
            td.className="PitaLabel";
            tr3.appendChild(td);

            notfirst=true;
            count_row++;
        }
    }

    //最終行（SUBTOTAL）
    tr = document.createElement("tr");
    table.appendChild(tr);
    //セル
    td = document.createElement("td");
    td.innerHTML="TOTAL";
    td.setAttribute("colspan",2);
    td.className="PitaLabel";
    tr.appendChild(td);
    for(var i = 0; i<subtotal_column.length; i++){
        td = document.createElement("td");
        td.innerHTML=subtotal_column[i];
        td.className="PitaLabel";
        tr.appendChild(td);
    }
    //総計
    td = document.createElement("td");
    td.innerHTML=total;
    td.className="PitaLabel";
    tr.appendChild(td);

    element = document.getElementById("PitaResult");
    element.appendChild(table);

    //Web Storageから前回選択セルを呼び出す
    var storage = sessionStorage;
    var value=storage.getItem(pkg+"."+cls);
    if(value!=null){
        var location=value.split(",");
        PitaResultExtract(location[0],location[1]);
    }
}

//INPUT:座標（集計数字セル左上を0,0とする）
function PitaResultExtract(column,row){
    //条件を満たしたテストのフルネーム
    var str=[];
    //抽出条件
    var condition="";
    if(row==-1){
        //上2段目セル
        condition=titleC1[column]+"."+titleC2[column];
        for(var i in dataTable[titleC1[column]][titleC2[column]]){
            for(var j in dataTable[titleC1[column]][titleC2[column]][i]){
                try{str=str.concat(dataTable[titleC1[column]][titleC2[column]][i][j][1]);}catch(e){}
            }
        }
    }else if(row==-2){
        //上1段目セル
        condition=titleC1[column];
        for(var i in dataTable[titleC1[column]]){
            for(var j in dataTable[titleC1[column]][i]){
                for(var k in dataTable[titleC1[column]][i][j]){
                    try{str=str.concat(dataTable[titleC1[column]][i][j][k][1]);}catch(e){}
                }
            }
        }
    }else if(column==-1){
        //左2列目セル
        condition=titleR1[row]+"."+titleR2[row];
        for(var i in dataTable){
            for(var j in dataTable[i]){
                try{str=str.concat(dataTable[i][j][titleR1[row]][titleR2[row]][1]);}catch(e){}
            }
        }
    }else if(column==-2){
        //左1列目セル
        condition=titleR1[row];
        for(var i in dataTable){
            for(var j in dataTable[i]){
                for (var k in dataTable[i][j][titleR1[row]]){
                    try{str=str.concat(dataTable[i][j][titleR1[row]][k][1]);}catch(e){}
                }
            }
        }
    }else if(column==-3 && row==-3) {
        //左上セル
        condition="all";
        for(var i in dataTable){
            for(var j in dataTable[i]){
                for (var k in dataTable[i][j]){
                    for (var l in dataTable[i][j][k]){
                        try{str=str.concat(dataTable[i][j][k][l][1]);}catch(e){}
                    }
                }
            }
        }
    }else{
        //通常セル
        condition=titleC1[column]+"."+titleC2[column]+"."+titleR1[row]+"."+titleR2[row];
        str = dataTable[titleC1[column]][titleC2[column]][titleR1[row]][titleR2[row]][1];
    }
    //抽出しているセルの色を変える．
    var element = document.getElementsByClassName("Pita_selected");
    if(element.item(0)!=null)element.item(0).classList.remove("Pita_selected");
    var element = document.getElementById("pitacell_"+column+"_"+row);
    element.classList.add("Pita_selected");

    //抽出条件の表示
    var element = document.getElementById("main-panel").getElementsByTagName("h2").item(0);
    element.innerHTML="Failed Test<br>(Filter:["+condition+"])";

    //失敗結果の一覧テーブルから抽出
    var element = document.getElementById("main-panel").getElementsByClassName("pane sortable bigtable").item(0);
    var elementTr=element.children.item(0);
    var elementTr=elementTr.children;
    for (var i = 0; i < elementTr.length; i++) {
        var elementTd=elementTr[i].children;
        var elementText=elementTd[0].getElementsByTagName("a");

        if(elementText[2]==null)continue;//タイトル行はNULL
        for(var key in str){
            if(elementText[2].innerHTML.indexOf(str[key])!=-1){
                elementTr[i].style.display="";
                break;
            }else{
                elementTr[i].style.display="none";
            }
        }
    }

    // Highlight images
    var tds = document.getElementById("attachments").getElementsByTagName("td");
    Array.from(tds).forEach(function(td) {
        if(td.getAttribute("name") != null) {
            if(condition == "all" || str.indexOf(td.getAttribute("name")) == -1) {
                td.classList.remove("hightlight");
            } else {
                td.classList.add("hightlight");
            }
        }
    });

    //Web Storageへ選択セルを保存
    var storage = sessionStorage;
    storage.setItem(pkg+"."+cls, column+","+row);

}
//PitaResult.js
//
//テスト結果のJson形式ファイルから，サマリーてーぶるを出力し，結果の抽出も行うスクリプト
//Jenkinsプラグインから生成された規定ののテスト結果のJsonファイルを読み込む．
//

function PitaResultShow(){

//縦横要素を求める
//{OS名:{ブラウザ:1}}
var column={};
//{Name:{Location:1}}
var row={};

var oss=[];
var browsers=[];
var names=[];
var locations=[];//TODO 絶対データ構造かえる．

//IDをエラーの情報と置き換える．
//{ID:[エラー名,エラー場所,プラットフォーム，ブラウザ]}nullは比較しない．
idToError={};
var idOfIdToError=0;

for(var i in data){
	//横軸（OS*Browser)
	if(data[i].os in column){
		if(data[i].browser in column[data[i].os]){
		}else{
			column[data[i].os][data[i].browser]=1;//数字はダミー
		}
	}else{
		column[data[i].os]={[data[i].browser]:1};
	}
	//縦軸（Name*Loc)
	if(data[i].errName in row){
		if(data[i].errLocation in row[data[i].errName]){
		}else{
			row[data[i].errName][data[i].errLocation]=1;
		}
	}else{
		row[data[i].errName]={[data[i].errLocation]:1};
	}
	if(oss.indexOf(data[i].os)<0){oss.push(data[i].os)}
	if(browsers.indexOf(data[i].browser)<0){browsers.push(data[i].browser)}
	if(names.indexOf(data[i].errName)<0){names.push(data[i].errName)}
	if(locations.indexOf(data[i].errLocation)<0){locations.push(data[i].errLocation)}
}

//結果てーぶる作る
var resultTree={};
for(var i = 0; i < Object.keys(oss).length; i++){
	resultTree[oss[i]]={};
	for(var j = 0; j < Object.keys(browsers).length; j++){
	resultTree[oss[i]][browsers[j]]={};
		for(var k = 0; k < Object.keys(names).length; k++){
			resultTree[oss[i]][browsers[j]][names[k]]={};
			for(var l = 0; l < Object.keys(locations).length; l++){
			resultTree[oss[i]][browsers[j]][names[k]][locations[l]]=0;
			}
		}
	}
}

//結果追加
for(var i in data){
	resultTree[data[i].os][data[i].browser][data[i].errName][data[i].errLocation]++;
}

//タグを吐く
table = document.createElement("table");
table.id="PitaResultTable";
tr = document.createElement("tr");
table.appendChild(tr);
td = document.createElement("td");
td.setAttribute("colspan",2);
td.setAttribute("rowspan",2);

		idOfIdToError++
		idToError[idOfIdToError]=[null,null,null,null];
		td.setAttribute("onClick", "PitaResultExtract(\""+idOfIdToError+"\")");
		td.id="Pita_"+idOfIdToError;

tr.appendChild(td);

tr2 = document.createElement("tr");
table.appendChild(tr2);


for(var i in column){
	if(i=="undefined")continue;//Jenkinsだと出るなぞのundefined対策
	td = document.createElement("td");
	td.innerHTML=i;

	idOfIdToError++
		idToError[idOfIdToError]=[null,null,i,null];
		td.setAttribute("onClick", "PitaResultExtract(\""+idOfIdToError+"\")");
		td.id="Pita_"+idOfIdToError;

	td.className = "PitaColumnLabel";
	td.setAttribute("colspan",Object.keys(column[i]).length);
	tr.appendChild(td);
	for(var j in column[i]){
		if(j=="undefined")continue;//Jenkinsだと出るなぞのundefined対策
		td = document.createElement("td");
		td.className = "PitaColumnLabel";
		td.innerHTML=j;

		idOfIdToError++
		idToError[idOfIdToError]=[null,null,i,j];
		td.setAttribute("onClick", "PitaResultExtract(\""+idOfIdToError+"\")");
		td.id="Pita_"+idOfIdToError;

		tr2.appendChild(td);
	}

}

//TOTAL
	td = document.createElement("td");
	td.innerHTML="TOTAL";
	td.className = "PitaColumnLabel";
	td.setAttribute("rowspan",2);
	tr.appendChild(td);

for(var i in row){
	if(i=="undefined")continue;//Jenkinsだと出るなぞのundefined対策
	tr = document.createElement("tr");
	table.appendChild(tr);
	td = document.createElement("td");
	td.setAttribute("rowspan",Object.keys(row[i]).length);
	if(i.indexOf("com.htmlhifive")==0){
	var str=i.substring(15);//行見出しからcom.htmlhifive.pitaliumは消して短くする
	}else{
	str=i;
	}
	td.innerHTML=str;

	idOfIdToError++
		idToError[idOfIdToError]=[i,null,null,null];
		td.setAttribute("onClick", "PitaResultExtract(\""+idOfIdToError+"\")");
		td.id="Pita_"+idOfIdToError;

	td.className = "PitaRowLabel";
	tr.appendChild(td)
	var times=0;
	for(var j in row[i]){
		if(times>0){
			tr = document.createElement("tr");
			table.appendChild(tr);
		}
		td = document.createElement("td");
		if(j.indexOf("com.htmlhifive")==0){
		var str=j.substring(15);//行見出しからcom.htmlhifive.pitaliumは消して短くする
		}else{
		str=j;
		}
		td.innerHTML=str;

		idOfIdToError++
		idToError[idOfIdToError]=[i,j,null,null];
		td.setAttribute("onClick", "PitaResultExtract(\""+idOfIdToError+"\")");
		td.id="Pita_"+idOfIdToError;

		td.className = "PitaRowLabel";
		tr.appendChild(td);
		times++;

		var rowTotal=0;
		for(var p in column){
		for(var q in column[p]){
			td = document.createElement("td");
			var cell_total=resultTree[p][q][i][j];
			if(cell_total>0){
				idOfIdToError++
				idToError[idOfIdToError]=[i,j,p,q];
				td.setAttribute("onClick", "PitaResultExtract(\""+idOfIdToError+"\")");
				td.id="Pita_"+idOfIdToError;
				td.innerHTML=cell_total;
			}
			rowTotal+=cell_total;
			tr.appendChild(td);
		}
		}
		tr.lastElementChild.remove();//なぞのundefined対策 TODO ここまでのundefined対策でフラグを導入しておき，立ってたら消す
		td = document.createElement("td");
			td.innerHTML=rowTotal;
			tr.appendChild(td);
	}

}
element = document.getElementById("PitaResult");
element.appendChild(table);
}



function PitaResultExtract(id){
var visibleItem={};
for(var p in data){
var tmp=0;
if(idToError[id][0]!=null){
	if(data[p].errName==idToError[id][0]){tmp++;}
}else{
	tmp++;
}
if(idToError[id][1]!=null){
	if(data[p].errLocation==idToError[id][1]){tmp++;}
}else{
	tmp++;
}
if(idToError[id][2]!=null){
	if(data[p].os==idToError[id][2]){tmp++;}
}else{
	tmp++;
}
if(idToError[id][3]!=null){
	if(data[p].browser==idToError[id][3]){tmp++;}
}else{
	tmp++;
}
if(tmp==4){visibleItem[data[p].testname]=1;}
}

//抽出条件の表示
var element = document.getElementById("main-panel").getElementsByTagName("h2").item(0);
element.innerHTML="Failed Test<br>(Filter:["+idToError[id]+"])";

//抽出しているセルの色を変える．
var element = document.getElementsByClassName("Pita_selected");
if(element.item(0)!=null)element.item(0).classList.remove("Pita_selected");

var element = document.getElementById("Pita_"+id);
element.classList.add("Pita_selected");

var element = document.getElementById("main-panel").getElementsByClassName("pane sortable bigtable").item(0);
var elementTr=element.children.item(0);
var elementTr=elementTr.children;
for (var i = 0; i < elementTr.length; i++) {
	var elementTd=elementTr[i].children;
	var elementText=elementTd[0].getElementsByTagName("a");

	if(elementText[2]==null)continue;//タイトル行はNULL
	for(key in visibleItem){
		if(elementText[2].innerHTML.indexOf(key)!=-1){
			elementTr[i].style.display="";
			break;
		}else{
			elementTr[i].style.display="none";
		}
	}
}


}
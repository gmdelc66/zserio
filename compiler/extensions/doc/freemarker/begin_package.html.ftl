<#ftl output_format="HTML">
<#include "doc_comment.inc.ftl">
<#include "symbol.inc.ftl">
<!doctype html>
<html lang="en">
  <head>
    <!-- Required meta tags -->
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <!-- Bootstrap CSS -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@4.5.3/dist/css/bootstrap.min.css" integrity="sha384-TX8t27EcRE3e/ihU7zmQxVncDAy5uIKz4rEkgIXeMed4M0jlfIDPvg6uqKI2xXr2" crossorigin="anonymous">

    <!-- Zserio Documentation CSS -->
    <link rel="stylesheet" href="../${stylesheetName}">

    <title>${symbol.name} documentation</title>
  </head>
  <body data-spy="scroll" data-target="#toc">
    <header id="header" class="navbar navbar-dark bg-dark sticky-top">
      <div class="navbar-brand">Zserio documentation for package ${symbol.name}</div>
    </header>
    <div class="container-fluid">
      <div class="row">
        <div id="symbol_overview" class="col-2">
          <nav class="nav flex-column">
<#list packages as pkg>
            <div>
              <@symbol_overview_package_link pkg.symbol symbol/>
    <#if pkg.packageSymbols?has_content>
              <nav class="nav flex-column collapse<#if symbol.name == pkg.symbol.name>.active</#if>">
        <#list pkg.packageSymbols as packageSymbol>
                <@symbol_overview_link packageSymbol.symbol packageSymbol.templateParameters/>
        </#list>
              </nav>
    </#if>
            </div>
</#list>
          </nav>
        </div>
        <main class="col" role="main">
          <h1 id="${symbol.htmlLink.htmlAnchor}" class="anchor">${symbol.name}</h1>
          <@doc_comments docComments 2, false/>

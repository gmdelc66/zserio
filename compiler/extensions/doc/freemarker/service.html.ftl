<#ftl output_format="HTML">
<#include "doc_comment.inc.ftl">
<#include "symbol.inc.ftl">
<#include "svg_diagram.inc.ftl">

    <h2 class="anchor" id="${symbol.htmlLink.htmlAnchor}">
<#if docComments.isDeprecated>
      <span class="deprecated">(deprecated) </span>
      <del><i>Service</i> ${symbol.name}</del>
<#else>
      <i>Service</i> ${symbol.name}
</#if>
    </h2>
    <@doc_comments docComments 2, false/>

    <table>
      <tr><td class="docuCode">
        <table>
          <tr><td>service ${symbol.name}</td></tr>
          <tr><td>{</td></tr>
<#list methodList as method>
          <tr><td class="tabIndent">
            <@symbol_reference method.responseSymbol/> <@symbol_reference method.symbol/><#rt>
              <#lt>(<@symbol_reference method.requestSymbol/>);
          </td></tr>
</#list>
          <tr><td>};</td></tr>
        </table>
      </td></tr>
    </table>
<#if methodList?has_content>

    <h3>Service methods</h3>

    <dl>
    <#list methodList as method>
      <dt class="memberItem"><a class="anchor" id="${method.symbol.htmlLink.htmlAnchor}">${method.symbol.name}:</a></dt>
      <dd class="memberDetail">
        <@doc_comments method.docComments, 4/>
      </dd>
    </#list>
    </dl>
</#if>
<#if collaborationDiagramSvg??>

    <@collaboration_diagram collaborationDiagramSvg/>
</#if>

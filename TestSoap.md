Write-Host "`n=== Test 1: Calculate ADD with Transaction ID ===" -ForegroundColor Cyan; $body1 = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:cal="http://example.com/calculator"
                  xmlns:hdr="http://example.com/headers">
   <soapenv:Header>
      <hdr:TransactionId>CALC-ADD-TEST-002</hdr:TransactionId>
   </soapenv:Header>
   <soapenv:Body>
      <cal:calculate>
         <cal:operand1>15</cal:operand1>
         <cal:operand2>0</cal:operand2>
         <cal:operation>DIVIDE</cal:operation>
      </cal:calculate>
   </soapenv:Body>
</soapenv:Envelope>
"@; $response1 = Invoke-WebRequest -Uri "http://localhost:8080/services/Calculator" -Method POST -ContentType "text/xml" -Body $body1; Write-Host $response1.Content -ForegroundColor Green
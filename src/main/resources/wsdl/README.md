# WSDL Files

Place your WSDL files in this directory.

## Generating Java Classes from WSDL

To generate Java classes from WSDL files:

1. Place your `.wsdl` files in this directory
2. Update the `pom.xml` file to configure the `cxf-codegen-plugin`
3. Run: `mvn clean generate-sources`

### Example Configuration

In `pom.xml`, uncomment and configure the wsdlOption:

```xml
<wsdlOption>
    <wsdl>${basedir}/src/main/resources/wsdl/YourService.wsdl</wsdl>
    <extraargs>
        <extraarg>-p</extraarg>
        <extraarg>com.example.generated</extraarg>
    </extraargs>
</wsdlOption>
```

The generated classes will be placed in: `target/generated-sources/cxf/`

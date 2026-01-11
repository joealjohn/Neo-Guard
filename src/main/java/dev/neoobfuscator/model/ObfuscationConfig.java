package dev.neoobfuscator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Configuration for an obfuscation job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObfuscationConfig {

    // Main package to obfuscate (others are exempted)
    private String mainPackage;

    // Target Java version
    @Builder.Default
    private String javaVersion = "17";

    // === Transformers ===

    // String encryption
    @Builder.Default
    private boolean stringEncryption = true;

    // Number encryption
    @Builder.Default
    private boolean numberEncryption = true;

    // Flow condition obfuscation
    @Builder.Default
    private boolean flowCondition = true;

    // Flow exception obfuscation
    @Builder.Default
    private boolean flowException = true;

    // Flow exception strength: LIGHT, NORMAL, AGGRESSIVE
    @Builder.Default
    private String flowExceptionStrength = "AGGRESSIVE";

    // Flow range obfuscation
    @Builder.Default
    private boolean flowRange = true;

    // Flow switch obfuscation
    @Builder.Default
    private boolean flowSwitch = true;

    // Ahegao trolling (adds fun elements)
    @Builder.Default
    private boolean ahegao = false;

    // Exempt patterns (classes/packages to skip)
    private List<String> exemptPatterns;

    /**
     * Generate HOCON config content for Skidfuscator.
     */
    public String toHocon() {
        StringBuilder sb = new StringBuilder();

        // Exemptions
        sb.append("exempt: [\n");
        if (mainPackage != null && !mainPackage.isEmpty()) {
            // Exempt everything except main package
            String escapedPackage = mainPackage.replace(".", "/");
            sb.append("    \"class{^(?!(").append(escapedPackage).append(")).*$}\"\n");
        }
        if (exemptPatterns != null) {
            for (String pattern : exemptPatterns) {
                sb.append("    \"").append(pattern).append("\"\n");
            }
        }
        sb.append("]\n\n");

        // Libraries
        sb.append("libs: []\n\n");

        // String Encryption
        sb.append("stringEncryption {\n");
        sb.append("    type: STANDARD\n");
        sb.append("    enabled: ").append(stringEncryption).append("\n");
        sb.append("    exempt: []\n");
        sb.append("}\n\n");

        // Number Encryption
        sb.append("numberEncryption {\n");
        sb.append("    enabled: ").append(numberEncryption).append("\n");
        sb.append("    exempt: []\n");
        sb.append("}\n\n");

        // Flow Condition
        sb.append("flowCondition {\n");
        sb.append("    enabled: ").append(flowCondition).append("\n");
        sb.append("    exempt: []\n");
        sb.append("}\n\n");

        // Flow Exception
        sb.append("flowException {\n");
        sb.append("    enabled: ").append(flowException).append("\n");
        sb.append("    strength: ").append(flowExceptionStrength).append("\n");
        sb.append("    exempt: []\n");
        sb.append("}\n\n");

        // Flow Range
        sb.append("flowRange {\n");
        sb.append("    enabled: ").append(flowRange).append("\n");
        sb.append("    exempt: []\n");
        sb.append("}\n\n");

        // Flow Switch
        sb.append("flowSwitch {\n");
        sb.append("    enabled: ").append(flowSwitch).append("\n");
        sb.append("    exempt: []\n");
        sb.append("}\n\n");

        // Ahegao
        sb.append("ahegao {\n");
        sb.append("    enabled: ").append(ahegao).append("\n");
        sb.append("    exempt: []\n");
        sb.append("}\n\n");

        // Driver (disabled by default)
        sb.append("driver: {\n");
        sb.append("    enabled: false\n");
        sb.append("}\n");

        return sb.toString();
    }
}

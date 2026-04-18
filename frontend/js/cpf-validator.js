// Utilitário para validação de CPF
const CPFValidator = {
    // Valida formato e verifica dígitos verificadores
    isValid: (cpf) => {
        // Remove caracteres especiais
        const cleanCPF = cpf.replace(/\D/g, '');
        
        // Verifica se tem 11 dígitos
        if (cleanCPF.length !== 11) return false;
        
        // Verifica se tem todos os dígitos iguais
        if (/^(\d)\1{10}$/.test(cleanCPF)) return false;
        
        // Calcula primeiro dígito verificador
        let sum = 0;
        for (let i = 0; i < 9; i++) {
            sum += parseInt(cleanCPF.charAt(i)) * (10 - i);
        }
        let firstDigit = 11 - (sum % 11);
        if (firstDigit > 9) firstDigit = 0;
        
        // Calcula segundo dígito verificador
        sum = 0;
        for (let i = 0; i < 10; i++) {
            sum += parseInt(cleanCPF.charAt(i)) * (11 - i);
        }
        let secondDigit = 11 - (sum % 11);
        if (secondDigit > 9) secondDigit = 0;
        
        // Compara os dígitos
        return parseInt(cleanCPF.charAt(9)) === firstDigit && 
               parseInt(cleanCPF.charAt(10)) === secondDigit;
    },
    
    // Formata CPF para XXX.XXX.XXX-XX
    format: (cpf) => {
        const cleanCPF = cpf.replace(/\D/g, '');
        if (cleanCPF.length !== 11) return cpf;
        return cleanCPF.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
    },
    
    // Remove formatação
    unformat: (cpf) => cpf.replace(/\D/g, '')
};

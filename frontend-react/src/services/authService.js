const mockUsersDatabase = [];

export const registerClient = async (userData) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const existingUser = mockUsersDatabase.find(u => u.email === userData.email);
      if (existingUser) {
        return reject(new Error('El correo electrónico ya se encuentra registrado.'));
      }

      const newUser = {
        ...userData,
        id: Date.now(),
        verified: false,
        token: '123456'
      };

      mockUsersDatabase.push(newUser);
      
      console.log('--- [MOCK EMAIL SENT] ---');
      console.log(`Para: ${userData.email}`);
      console.log(`Código de verificación: 123456`);

      resolve({
        success: true,
        message: 'Usuario registrado correctamente. Revisa tu correo.',
        email: userData.email
      });
    }, 1500);
  });
};

export const verifyEmailToken = async (token) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (token === '123456') {
        resolve({ success: true, message: 'Correo verificado exitosamente.' });
      } else {
        reject(new Error('El código ingresado es incorrecto. Usa: 123456'));
      }
    }, 1000);
  });
};
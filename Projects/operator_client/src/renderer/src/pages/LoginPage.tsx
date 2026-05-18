import {
  InputAdornment,
  IconButton,
  OutlinedInput,
  InputLabel,
  FormControl,
  FormHelperText,
  Typography,
  Paper,
  Box,
  Stack
} from '@mui/material';

import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';

import React, { MouseEvent, useId, useState } from 'react';

import MyButton from '@renderer/components/MyButton';
import { authApi } from '@renderer/api/auth.api';
import { tokenService } from '@renderer/services/token.service';
import { useNavigate } from 'react-router-dom';

function LoginPage(): React.JSX.Element {

  const navigate = useNavigate();

  const [emailError, setEmailError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const emailId = useId();
  const passwordId = useId();

  const [showPassword, setShowPassword] = useState(false);

  const validate = (): boolean => {
    let isValid = true;

    setEmailError(null);
    setPasswordError(null);

    if (!email.trim()) {
      setEmailError('Введите email');
      isValid = false;
    } else if (
      !/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(email)
    ) {
      setEmailError('Некорректный email');
      isValid = false;
    }

    if (!password.trim()) {
      setPasswordError('Введите пароль');
      isValid = false;
    } else if (password.length < 8) {
      setPasswordError('Минимум 8 символов');
      isValid = false;
    }

    return isValid;
};

  const login = async (): Promise<void> => {
    const isValid = validate();

    if (!isValid) {
      return;
    }

    try {
      const response = await authApi.login({email, password});

      tokenService.setAccessToken(
        response.accessToken
      );

      tokenService.setRefreshToken(
        response.refreshToken
      );
      
      navigate('/monitoring');
    } catch (error) {
      setPasswordError(
        'Неверный email или пароль'
      );
    }
  }

  const handleClickShowPassword = (): void => {
    setShowPassword((show) => !show);
  };

  const handleMouseDownPassword = (
    event: MouseEvent<HTMLButtonElement>
  ): void => {
    event.preventDefault();
  };

  const handleMouseUpPassword = (
    event: MouseEvent<HTMLButtonElement>
  ): void => {
    event.preventDefault();
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#f4f6f8',
        p: 2
      }}
    >
      <Paper
        elevation={4}
        sx={{
          width: 400,
          p: 4,
          borderRadius: 4
        }}
      >
        <Typography
          variant="h4"
          fontWeight={700}
          mb={1}
        >
          Вход
        </Typography>

        <Typography
          variant="body2"
          color="text.secondary"
          mb={4}
        >
          Введите данные для входа в систему
        </Typography>

        <Stack spacing={3} sx={{mt: 2}}>
          <FormControl
            error={Boolean(emailError)}
            fullWidth
            variant="outlined"
          >
            <InputLabel htmlFor={`${emailId}-input`}>
              Email
            </InputLabel>

            <OutlinedInput
              id={`${emailId}-input`}
              type="email"
              label="Email"
              onChange={(e) => setEmail(e.target.value)}
            />

            {emailError && (
              <FormHelperText>
                {emailError}
              </FormHelperText>
            )}
          </FormControl>

          <FormControl
            error={Boolean(passwordError)}
            fullWidth
            variant="outlined"
          >
            <InputLabel htmlFor={`${passwordId}-input`}>
              Password
            </InputLabel>

            <OutlinedInput
              id={`${passwordId}-input`}
              type={showPassword ? 'text' : 'password'}
              label="Password"
              onChange={(e) => setPassword(e.target.value)}
              endAdornment={
                <InputAdornment position="end">
                  <IconButton
                    onClick={handleClickShowPassword}
                    onMouseDown={handleMouseDownPassword}
                    onMouseUp={handleMouseUpPassword}
                    edge="end"
                  >
                    {showPassword
                      ? <VisibilityOff />
                      : <Visibility />}
                  </IconButton>
                </InputAdornment>
              }
            />

            {passwordError && (
              <FormHelperText>
                {passwordError}
              </FormHelperText>
            )}
          </FormControl>

          <MyButton type="submit" onClick={login}>
            Войти
          </MyButton>
        </Stack>
      </Paper>
    </Box>
  );
}

export default LoginPage;
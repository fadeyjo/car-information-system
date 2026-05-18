import React from 'react';
import { Button } from '@mui/material';

interface MyButtonProps {
  children: React.ReactNode;
  type?: 'button' | 'submit' | 'reset';
  onClick: () => void
}

function MyButton({
  children,
  type = 'button',
  onClick
}: MyButtonProps): React.JSX.Element {
  return (
    <Button
      type={type}
      variant="contained"
      fullWidth
      size="large"
      onClick={onClick}
      sx={{
        mt: 1,
        height: 48,
        borderRadius: 2,
        textTransform: 'none',
        fontSize: '1rem'
      }}
    >
      {children}
    </Button>
  );
}

export default MyButton;
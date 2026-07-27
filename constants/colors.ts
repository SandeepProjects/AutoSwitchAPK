export interface ColorPalette {
  background: string;
  card: string;
  foreground: string;
  primary: string;
  accent: string;
  success: string;
  destructive: string;
  warning: string;
  mutedForeground: string;
  border: string;
}

export const colors: { dark: ColorPalette; light: ColorPalette } = {
  dark: {
    background: '#050B18',
    card: '#0D1526',
    foreground: '#F0F4FF',
    primary: '#00C8FF',
    accent: '#00E87A',
    success: '#00E87A',
    destructive: '#FF3B3B',
    warning: '#FFB020',
    mutedForeground: '#8896A8',
    border: '#1A2B45',
  },
  light: {
    background: '#F0F4FF',
    card: '#FFFFFF',
    foreground: '#0F172A',
    primary: '#0062FF',
    accent: '#00C97A',
    success: '#00C97A',
    destructive: '#EF4444',
    warning: '#F59E0B',
    mutedForeground: '#64748B',
    border: '#E2E8F0',
  },
};
